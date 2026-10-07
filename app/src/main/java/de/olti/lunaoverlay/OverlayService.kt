package de.olti.lunaoverlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Service
import android.content.Intent
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView

class OverlayService : Service() {
    enum class LunaState(val drawable: Int) {
        NEUTRAL(R.drawable.luna_waiting),
        SMILING(R.drawable.luna_listening),
        THINKING(R.drawable.luna_thinking),
        SPEAKING(R.drawable.luna_responding)
    }

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var imageView: ImageView? = null
    private var ambientAnimation: ObjectAnimator? = null
    private var stateAnimation: AnimatorSet? = null
    private val handler = Handler(Looper.getMainLooper())
    private var state = LunaState.NEUTRAL

    // Temporary demo sequence. Later this same setState() entry point can be
    // driven by ChatGPT/TTS events and real lip-sync data.
    private val demoStates = arrayOf(
        LunaState.NEUTRAL,
        LunaState.SMILING,
        LunaState.THINKING,
        LunaState.SPEAKING
    )
    private var demoIndex = 0

    private val demoCycle = object : Runnable {
        override fun run() {
            demoIndex = (demoIndex + 1) % demoStates.size
            setState(demoStates[demoIndex])
            handler.postDelayed(this, if (state == LunaState.SPEAKING) 5200L else 4000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = (160 * resources.displayMetrics.density).toInt()

        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(state.drawable)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }

        params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 180
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        imageView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    imageView?.let { wm.updateViewLayout(it, params) }
                    true
                }
                else -> false
            }
        }

        wm.addView(imageView, params)
        startAmbientBreathing()
        setState(LunaState.NEUTRAL, animateTransition = false)
        handler.postDelayed(demoCycle, 3500)
    }

    private fun startAmbientBreathing() {
        val v = imageView ?: return
        ambientAnimation?.cancel()
        ambientAnimation = ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.018f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.022f, 1f)
        ).apply {
            duration = 2800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    fun setState(newState: LunaState, animateTransition: Boolean = true) {
        val v = imageView ?: return
        state = newState
        stateAnimation?.cancel()
        v.animate().cancel()

        val swap = {
            v.setImageResource(newState.drawable)
            runStateMotion(v, newState)
        }

        if (animateTransition) {
            v.animate().alpha(0.35f).setDuration(120).withEndAction {
                swap()
                v.animate().alpha(1f).setDuration(180).start()
            }.start()
        } else {
            swap()
            v.alpha = 1f
        }
    }

    private fun runStateMotion(v: View, newState: LunaState) {
        v.rotation = 0f
        v.translationX = 0f
        v.translationY = 0f

        val motion = when (newState) {
            LunaState.NEUTRAL -> ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f, -2f, 0f).apply {
                duration = 2400
            }
            LunaState.SMILING -> ObjectAnimator.ofFloat(v, View.ROTATION, 0f, -1.8f, 1.2f, 0f).apply {
                duration = 1100
            }
            LunaState.THINKING -> ObjectAnimator.ofFloat(v, View.ROTATION, 0f, -2.8f, -1.8f, 0f).apply {
                duration = 1800
            }
            LunaState.SPEAKING -> ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0f, 2.5f, -1.5f, 0f).apply {
                duration = 900
                repeatCount = 4
            }
        }

        val nod = if (newState == LunaState.SPEAKING) {
            ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f, 2.5f, 0f).apply {
                duration = 700
                repeatCount = 5
            }
        } else null

        stateAnimation = AnimatorSet().apply {
            if (nod != null) playTogether(motion, nod) else play(motion)
            start()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        ambientAnimation?.cancel()
        stateAnimation?.cancel()
        imageView?.animate()?.cancel()
        imageView?.let { wm.removeView(it) }
        imageView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

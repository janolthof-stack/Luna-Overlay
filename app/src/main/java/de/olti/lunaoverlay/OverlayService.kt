package de.olti.lunaoverlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Service
import android.content.Intent
import android.graphics.Outline
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
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
import android.content.Context

class OverlayService : Service() {
    enum class LunaState(val drawable: Int) {
        NEUTRAL(R.drawable.luna_waiting),
        SMILING(R.drawable.luna_listening),
        THINKING(R.drawable.luna_thinking),
        SPEAKING(R.drawable.luna_responding)
    }

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var imageView: LunaImageView? = null
    private var ambientAnimation: ObjectAnimator? = null
    private var stateAnimation: AnimatorSet? = null
    private var frameAnimation: AnimationDrawable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var state = LunaState.NEUTRAL

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
        frameAnimation?.stop()
        frameAnimation = null
        v.animate().cancel()

        val swap = {
            v.setImageResource(newState.drawable)
            val drawable = v.drawable
            if (drawable is AnimationDrawable) {
                frameAnimation = drawable
                v.post { drawable.start() }
            }
            v.listening = newState == LunaState.SMILING
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

    private fun stateFromAction(action: String?): LunaState? = when (action) {
        ACTION_LISTENING -> LunaState.SMILING
        ACTION_THINKING -> LunaState.THINKING
        ACTION_SPEAKING -> LunaState.SPEAKING
        ACTION_IDLE -> LunaState.NEUTRAL
        else -> null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stateFromAction(intent?.action)?.let { setState(it) }
        return START_STICKY
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


    private class LunaImageView(context: Context) : ImageView(context) {
        var listening: Boolean = false
            set(value) {
                field = value
                blinkStart = if (value) System.currentTimeMillis() else 0L
                invalidate()
            }
        private val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(128, 82, 68) }
        private var blinkStart = 0L

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!listening) return
            val cycle = ((System.currentTimeMillis() - blinkStart) % 2600L).toFloat()
            val amount = when {
                cycle < 2050f -> 0f
                cycle < 2110f -> (cycle - 2050f) / 60f
                cycle < 2190f -> 1f
                cycle < 2250f -> 1f - (cycle - 2190f) / 60f
                else -> 0f
            }
            if (amount > 0f) {
                val sx = width / 320f
                val sy = height / 238f
                fun lid(l: Float, t: Float, r: Float, b: Float) {
                    val cy = (t + b) / 2f
                    val half = (b - t) * amount / 2f
                    canvas.drawRoundRect(l*sx, (cy-half)*sy, r*sx, (cy+half)*sy, 3f*sx, 3f*sy, lidPaint)
                }
                lid(103f, 78f, 137f, 91f)
                lid(166f, 77f, 200f, 90f)
            }
            postInvalidateDelayed(16L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        ambientAnimation?.cancel()
        stateAnimation?.cancel()
        frameAnimation?.stop()
        imageView?.animate()?.cancel()
        imageView?.let { wm.removeView(it) }
        imageView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_LISTENING = "de.olti.lunaoverlay.action.LISTENING"
        const val ACTION_THINKING = "de.olti.lunaoverlay.action.THINKING"
        const val ACTION_SPEAKING = "de.olti.lunaoverlay.action.SPEAKING"
        const val ACTION_IDLE = "de.olti.lunaoverlay.action.IDLE"
    }
}


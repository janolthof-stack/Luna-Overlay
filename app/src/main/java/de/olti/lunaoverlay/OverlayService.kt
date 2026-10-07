package de.olti.lunaoverlay

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
    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var imageView: ImageView? = null
    private var breathe: ObjectAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val images = intArrayOf(
        R.drawable.luna_waiting,
        R.drawable.luna_listening,
        R.drawable.luna_thinking,
        R.drawable.luna_responding
    )
    private var index = 0

    private val cycle = object : Runnable {
        override fun run() {
            val v = imageView ?: return
            v.animate().alpha(0.15f).setDuration(180).withEndAction {
                index = (index + 1) % images.size
                v.setImageResource(images[index])
                v.animate().alpha(1f).setDuration(220).start()
            }.start()
            handler.postDelayed(this, 4000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = (160 * resources.displayMetrics.density).toInt()

        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(images[0])
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

        imageView?.let { v ->
            breathe = ObjectAnimator.ofPropertyValuesHolder(
                v,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.025f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.025f, 1f)
            ).apply {
                duration = 2600
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }
        handler.postDelayed(cycle, 4000)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        breathe?.cancel()
        imageView?.animate()?.cancel()
        imageView?.let { wm.removeView(it) }
        imageView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

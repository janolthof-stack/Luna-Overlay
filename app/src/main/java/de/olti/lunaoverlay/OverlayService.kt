package de.olti.lunaoverlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var view: TextView? = null
    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        view = TextView(this).apply {
            text = "LUNA\n🙂"
            textSize = 26f
            setPadding(24,24,24,24)
            setBackgroundColor(0xCCFFFFFF.toInt())
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 180 }
        wm.addView(view, p)
    }
    override fun onDestroy() { view?.let { wm.removeView(it) }; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}

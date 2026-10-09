package de.olti.lunaoverlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView

/** Local, read-only observer. No microphone, gestures, network, or transcript storage. */
class LunaAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: LinearLayout? = null
    private var image: LunaAvatarView? = null
    private var caption: TextView? = null
    private var lastState: ChatGptStateDetector.State? = null
    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != CHATGPT_PACKAGE) return
        // Fixed polling cannot be starved by continuous token/content events.
        // No extra traversal per event: changing transcripts can produce many events.
    }

    private fun refresh() {
        if (!(getSystemService(POWER_SERVICE) as PowerManager).isInteractive) {
            removeOverlay()
            return
        }
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != CHATGPT_PACKAGE) {
            root?.recycle()
            removeOverlay()
            return
        }
        val labels = mutableListOf<String>()
        var budget = 200
        fun collect(node: AccessibilityNodeInfo) {
            if (--budget < 0 || !node.isVisibleToUser) return
            val kind = node.className?.toString().orEmpty()
            val id = node.viewIdResourceName?.substringAfterLast('/')?.lowercase().orEmpty()
            val isControl = node.isClickable || kind.endsWith("Button") || kind.endsWith("ProgressBar")
            val isStatus = id == "status" || id.endsWith("_status") || id == "voice_state"
            if ((isControl || isStatus) && node.isEnabled && !node.isEditable) {
                node.contentDescription?.toString()?.takeIf { it.length <= 80 }?.let(labels::add)
                node.text?.toString()?.takeIf { it.length <= 80 }?.let(labels::add)
            }
            for (index in 0 until node.childCount) {
                if (budget <= 0) break
                val child = node.getChild(index) ?: continue
                try { collect(child) } finally { child.recycle() }
            }
        }
        try { collect(root) } finally { root.recycle() }
        val state = ChatGptStateDetector.detect(labels)
        if (overlay == null && !createOverlay()) return
        if (state == lastState) return
        lastState = state
        val animated = image?.showState(state) == true
        val message = when (state) {
            ChatGptStateDetector.State.IDLE -> "Bereit"
            ChatGptStateDetector.State.LISTENING -> "Zuhören erkannt"
            ChatGptStateDetector.State.THINKING -> "Verarbeitung erkannt"
            ChatGptStateDetector.State.SPEAKING -> "Sprachausgabe erkannt"
            ChatGptStateDetector.State.UNKNOWN -> "Kein eindeutiges Zustandssignal"
        }
        val status = if (animated) message else "$message · Animation fehlt"
        caption?.text = status
        getSharedPreferences("luna_status", MODE_PRIVATE).edit().putString("last_status", status).apply()
    }

    private fun createOverlay(): Boolean {
        val size = (160 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD202020.toInt())
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        image = LunaAvatarView(this)
        caption = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
        }
        layout.addView(image, LinearLayout.LayoutParams(size, size))
        layout.addView(caption, LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.WRAP_CONTENT))
        val params = WindowManager.LayoutParams(size, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 24
                y = 180
            }
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        layout.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    wm.updateViewLayout(layout, params)
                    true
                }
                else -> true
            }
        }
        return try {
            wm.addView(layout, params)
            overlay = layout
            lastState = null
            true
        } catch (_: RuntimeException) {
            image = null; caption = null
            getSharedPreferences("luna_status", MODE_PRIVATE).edit()
                .putString("last_status", "Overlay konnte nicht geöffnet werden").apply()
            false
        }
    }

    private fun removeOverlay() {
        overlay?.let { wm.removeView(it) }
        overlay = null; image = null; caption = null; lastState = null
    }

    override fun onInterrupt() {
        handler.removeCallbacks(tick)
        removeOverlay()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        super.onDestroy()
    }

    companion object { const val CHATGPT_PACKAGE = "com.openai.chatgpt" }
}

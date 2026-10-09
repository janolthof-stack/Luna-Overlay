package de.olti.lunaoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.os.SystemClock
import android.widget.ImageView
import java.io.IOException

/** Plays bundled frame animation; never simulates a conversation or animates a still photo. */
@Suppress("DEPRECATION")
class LunaAvatarView(context: Context) : ImageView(context) {
    private var movie: Movie? = null
    private var startedAt = 0L

    init {
        scaleType = ScaleType.CENTER_CROP
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun showState(state: ChatGptStateDetector.State): Boolean {
        val name = when (state) {
            ChatGptStateDetector.State.LISTENING -> "listening"
            ChatGptStateDetector.State.THINKING -> "thinking"
            ChatGptStateDetector.State.SPEAKING -> "speaking"
            else -> "idle"
        }
        movie = try {
            context.assets.open("luna/$name.gif").use { Movie.decodeStream(it) }
                ?.takeIf { it.width() > 0 && it.height() > 0 && it.duration() > 0 }
        } catch (_: IOException) { null }
        startedAt = SystemClock.uptimeMillis()
        val fallback = when (state) {
            ChatGptStateDetector.State.LISTENING -> R.drawable.luna_listening
            ChatGptStateDetector.State.THINKING -> R.drawable.luna_thinking
            ChatGptStateDetector.State.SPEAKING -> R.drawable.luna_responding
            else -> R.drawable.luna_waiting
        }
        setImageResource(fallback)
        invalidate()
        return movie != null
    }

    override fun onDraw(canvas: Canvas) {
        val clip = movie
        if (clip == null) {
            super.onDraw(canvas)
            return
        }
        if (width <= 0 || height <= 0) return
        clip.setTime(((SystemClock.uptimeMillis() - startedAt) % clip.duration()).toInt())
        val scale = maxOf(width.toFloat() / clip.width(), height.toFloat() / clip.height())
        val save = canvas.save()
        canvas.translate((width - clip.width() * scale) / 2f, (height - clip.height() * scale) / 2f)
        canvas.scale(scale, scale)
        clip.draw(canvas, 0f, 0f)
        canvas.restoreToCount(save)
        if (isAttachedToWindow && windowVisibility == VISIBLE) postInvalidateOnAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidate()
    }
}

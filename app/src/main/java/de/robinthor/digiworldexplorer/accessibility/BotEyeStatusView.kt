package de.robinthor.digiworldexplorer.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.View
import de.robinthor.digiworldexplorer.automation.DirectorSnapshot
import de.robinthor.digiworldexplorer.automation.ObservedScreen

/** Coloured eyes turn the launcher mascot into a glanceable automation-state indicator. */
class BotEyeStatusView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var snapshot = DirectorSnapshot()
    private var glance = 0
    private val animate = object : Runnable {
        override fun run() {
            if (!isSearching()) return
            glance = (glance + 1) % 3
            invalidate()
            handler.postDelayed(this, 360L)
        }
    }

    fun update(value: DirectorSnapshot) {
        snapshot = value
        handler.removeCallbacks(animate)
        glance = 0
        if (isSearching()) handler.postDelayed(animate, 360L)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(animate)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val cy = 27f * d
        val left = 20f * d
        val right = 32f * d
        if (!AutomationStateBridge.enabled(snapshot)) {
            paint.color = Color.rgb(75, 82, 91); paint.strokeWidth = 2.5f * d; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(left - 3f * d, cy, left + 3f * d, cy, paint)
            canvas.drawLine(right - 3f * d, cy, right + 3f * d, cy, paint)
            return
        }
        val error = snapshot.state == "Paused" || snapshot.screen == ObservedScreen.CAPTURE_BLOCKED ||
            snapshot.action.contains("paused", true) || snapshot.action.contains("error", true)
        val color = when {
            error -> Color.rgb(239, 74, 82)
            snapshot.screen != ObservedScreen.UNKNOWN -> Color.rgb(55, 224, 143)
            isSearching() -> Color.rgb(250, 190, 48)
            else -> Color.rgb(145, 153, 164)
        }
        val offset = if (isSearching()) (glance - 1) * 1.7f * d else 0f
        paint.color = Color.argb(210, 7, 16, 25); paint.style = Paint.Style.FILL
        canvas.drawCircle(left, cy, 4.7f * d, paint); canvas.drawCircle(right, cy, 4.7f * d, paint)
        paint.color = color
        canvas.drawCircle(left + offset, cy, 2.7f * d, paint); canvas.drawCircle(right + offset, cy, 2.7f * d, paint)
    }

    private fun isSearching() = snapshot.state == "Watching" && snapshot.screen == ObservedScreen.UNKNOWN && snapshot.confidence > 0

    /** Keeps this view independent of mutable global state while still supporting the explicit off snapshot. */
    private object AutomationStateBridge {
        fun enabled(snapshot: DirectorSnapshot) = snapshot.state != "Automation off"
    }
}

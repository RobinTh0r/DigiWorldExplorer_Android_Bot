package de.robinthor.digiworldexplorer.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.View
import de.robinthor.digiworldexplorer.automation.BotEyeState
import de.robinthor.digiworldexplorer.automation.BotEyeStateResolver
import de.robinthor.digiworldexplorer.automation.DirectorSnapshot
import kotlin.math.sin

/** Animated mascot eyes: closed/off, green/active, yellow/search, gray/unknown, red/error. */
class BotEyeStatusView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var eyeState = BotEyeState.UNKNOWN
    private var frame = 0
    private val animate = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow || !eyeState.animated) return
            frame = (frame + 1) % 24
            invalidate()
            handler.postDelayed(this, eyeState.frameDelay)
        }
    }

    fun update(value: DirectorSnapshot) {
        val next = BotEyeStateResolver.resolve(value)
        if (next != eyeState) frame = 0
        eyeState = next
        handler.removeCallbacks(animate)
        if (eyeState.animated) handler.postDelayed(animate, eyeState.frameDelay)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (eyeState.animated) handler.postDelayed(animate, eyeState.frameDelay)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(animate)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val cy = 27f * d
        val left = 20f * d
        val right = 32f * d
        paint.style = Paint.Style.FILL

        if (eyeState == BotEyeState.OFF) {
            paint.color = Color.rgb(75, 82, 91)
            paint.strokeWidth = 2.5f * d
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(left - 3f*d, cy, left + 3f*d, cy, paint)
            canvas.drawLine(right - 3f*d, cy, right + 3f*d, cy, paint)
            return
        }

        val blink = eyeState == BotEyeState.ACTIVE && frame in 20..21
        val radiusY = when { blink -> .55f*d; eyeState == BotEyeState.UNKNOWN -> 2.7f*d; else -> 4.7f*d }
        paint.color = Color.argb(220, 7, 16, 25)
        canvas.drawOval(left-4.7f*d, cy-radiusY, left+4.7f*d, cy+radiusY, paint)
        canvas.drawOval(right-4.7f*d, cy-radiusY, right+4.7f*d, cy+radiusY, paint)
        if (blink) return

        val color = when (eyeState) {
            BotEyeState.ACTIVE -> Color.rgb(55, 224, 143)
            BotEyeState.SEARCHING -> Color.rgb(250, 190, 48)
            BotEyeState.UNKNOWN -> Color.rgb(145, 153, 164)
            BotEyeState.ERROR -> Color.rgb(239, 74, 82)
            BotEyeState.OFF -> Color.TRANSPARENT
        }
        val xOffset = if (eyeState == BotEyeState.SEARCHING) ((frame % 3) - 1) * 1.9f*d else 0f
        val radius = when (eyeState) {
            BotEyeState.ERROR -> (2.25f + .55f * ((sin(frame*.8) + 1.0) / 2.0)).toFloat()
            BotEyeState.ACTIVE -> 2.65f
            BotEyeState.UNKNOWN -> 2.1f
            BotEyeState.SEARCHING -> 2.7f
            BotEyeState.OFF -> 0f
        } * d
        paint.color = color
        canvas.drawCircle(left+xOffset, cy, radius, paint)
        canvas.drawCircle(right+xOffset, cy, radius, paint)

        if (eyeState == BotEyeState.ERROR) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f*d
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(left-4f*d, cy-6f*d, left+3f*d, cy-4.5f*d, paint)
            canvas.drawLine(right-3f*d, cy-4.5f*d, right+4f*d, cy-6f*d, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private val BotEyeState.animated get() = this == BotEyeState.ACTIVE || this == BotEyeState.SEARCHING || this == BotEyeState.ERROR
    private val BotEyeState.frameDelay get() = when (this) {
        BotEyeState.SEARCHING -> 340L
        BotEyeState.ERROR -> 180L
        BotEyeState.ACTIVE -> 140L
        else -> 600L
    }
}

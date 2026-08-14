package de.robinthor.digiworldexplorer.feed

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.input.SafeTapRandomizer
import de.robinthor.digiworldexplorer.strategy.AutomationState

/** Conservative detector for the small white food bubble on the main battle screen. */
object FeedFrameAnalyzer {
    private var stableFrames = 0
    private var mainScreenFrames = 0
    private var lastX = 0f
    private var lastY = 0f
    private var tappingUntil = 0L
    private var nextTapAt = 0L
    private var tapsLeft = 0
    private var cooldownUntil = 0L

    fun reset() { stableFrames = 0; mainScreenFrames = 0; tappingUntil = 0L; nextTapAt = 0L; tapsLeft = 0; cooldownUntil = 0L }

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.autoFeedEnabled) { reset(); return false }
        val now=SystemClock.elapsedRealtime()
        if (progressSequence(now)) return true
        val plane = image.planes.firstOrNull() ?: return false
        val buffer = plane.buffer
        fun rgb(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }

        fun sampleRatio(l:Float,t:Float,r:Float,b:Float,steps:Int,p:(Int)->Boolean):Double {
            var matches=0; var total=0
            for (iy in 0 until steps) for (ix in 0 until steps) {
                val x=(width*(l+(r-l)*(ix+.5f)/steps)).toInt().coerceIn(0,width-1)
                val y=(height*(t+(b-t)*(iy+.5f)/steps)).toInt().coerceIn(0,height-1)
                total++; if(p(rgb(x,y)))matches++
            }
            return matches.toDouble()/total.coerceAtLeast(1)
        }

        // Main screen fingerprint: dark-blue bottom control deck and no cyan Partner header.
        val bottomDark = sampleRatio(.18f, .76f, .82f, .92f, 12) { c ->
            Color.blue(c) > Color.red(c) * 1.25 && Color.blue(c) > Color.green(c) * 1.05 && Color.blue(c) < 150
        }
        val headerCyan = sampleRatio(.18f, .13f, .82f, .18f, 8) { c ->
            Color.blue(c) > 145 && Color.green(c) > 105 && Color.red(c) < 85
        }
        if (bottomDark < .30 || headerCyan > .32) { stableFrames = 0; mainScreenFrames = 0; return false }
        mainScreenFrames++

        var white = 0
        var sx = 0L
        var sy = 0L
        val x0 = (width * .45f).toInt(); val x1 = (width * .60f).toInt()
        val y0 = (height * .29f).toInt(); val y1 = (height * .40f).toInt()
        val step = (width / 180).coerceAtLeast(3)
        for (y in y0 until y1 step step) for (x in x0 until x1 step step) {
            val c = rgb(x, y); val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            if (r > 205 && g > 205 && b > 205 && maxOf(r,g,b)-minOf(r,g,b) < 28) { white++; sx += x; sy += y }
        }
        val bubblePresent = white in 25..180
        // At 720x1280 the antialiased food bubble contributes about 488 samples.
        // Main-screen recognition and the two-frame position lock remain the safety gates.
        if (!bubblePresent) { stableFrames = 0; return true }
        val cx = sx.toFloat() / white; val cy = sy.toFloat() / white
        if (kotlin.math.abs(cx-lastX) < width*.04f && kotlin.math.abs(cy-lastY) < height*.035f) stableFrames++ else stableFrames=1
        lastX=(cx+width*.015f).coerceAtMost(width*.61f); lastY=(cy+height*.010f).coerceIn(height*.28f,height*.41f)
        if (stableFrames >= 4 && mainScreenFrames >= 4 && tapsLeft == 0 && now >= cooldownUntil) {
            tapsLeft = 3; tappingUntil = now + 3_000L; nextTapAt = now
        }
        return true
    }

    fun allowImmediateScan() {
        stableFrames = 0
        mainScreenFrames = 0
        cooldownUntil = 0L
    }

    fun pauseForDigiWorld() {
        stableFrames = 0
        mainScreenFrames = 0
        tappingUntil = 0L
        nextTapAt = 0L
        tapsLeft = 0
    }
    private fun progressSequence(now:Long):Boolean {
        if (tapsLeft > 0) {
            if (now > tappingUntil) { tapsLeft=0; cooldownUntil=now+60_000L; return true }
            if (now >= nextTapAt) {
                tapsLeft--
                nextTapAt=now+SafeTapRandomizer.delay(520L,230L)
                DigiWorldAccessibilityService.instance?.apply {
                    updateStatusKeepingGrid(getString(R.string.overlay_auto_feed),true)
                    dispatchSafeRandomizedTap(lastX,lastY) { }
                }
                if (tapsLeft==0) cooldownUntil=now+60_000L
            }
            return true
        }
        return false
    }

}

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
    fun isBusy(): Boolean = tapsLeft > 0
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
        if (!AutomationState.enabled) { reset(); return false }
        val now=SystemClock.elapsedRealtime()
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
        if (bottomDark < .30 || headerCyan > .32 ||
            !de.robinthor.digiworldexplorer.automation.HomeScreenDetector.detect(width, height, ::rgb)) {
            stableFrames = 0; mainScreenFrames = 0; return false
        }
        mainScreenFrames++

        val frame = de.robinthor.digiworldexplorer.vision.PixelFrame(width, height) { x,y -> rgb(x,y) }
        val bubble = BondBubbleDetector.detect(frame)
        if (bubble == null) { stableFrames = 0; tapsLeft = 0; return true }
        val (px,py) = de.robinthor.digiworldexplorer.vision.GameViewport.fit(width,height).pixel(bubble)
        val cx = px.toFloat(); val cy = py.toFloat()
        if (kotlin.math.abs(cx-lastX) < width*.04f && kotlin.math.abs(cy-lastY) < height*.035f) stableFrames++ else stableFrames=1
        lastX=cx; lastY=cy
        if (progressSequence(now)) return true
        if (stableFrames >= 4 && mainScreenFrames >= 4 && tapsLeft == 0 && now >= cooldownUntil) {
            tapsLeft = 1; tappingUntil = now + 3_000L; nextTapAt = now
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
                    android.util.Log.i("DigiWorldBond", "collect bubble=$lastX,$lastY")
                    dispatchSafeRandomizedTap(lastX,lastY) { }
                }
                if (tapsLeft==0) cooldownUntil=now+60_000L
            }
            return true
        }
        return false
    }

}

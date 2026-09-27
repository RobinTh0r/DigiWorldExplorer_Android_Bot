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
    private var lastBubbleSeenAt = 0L
    @Volatile private var lastCollectedAt = 0L

    fun reset() { stableFrames = 0; mainScreenFrames = 0; tappingUntil = 0L; nextTapAt = 0L; tapsLeft = 0; cooldownUntil = 0L; lastBubbleSeenAt = 0L; lastCollectedAt = 0L }
    fun collectedSince(since: Long): Boolean = lastCollectedAt >= since && since > 0L
    fun collectionSettledSince(since: Long, now: Long, settleMillis: Long = 1_000L): Boolean =
        collectedSince(since) && now - lastCollectedAt >= settleMillis

    fun analyze(image: Image, width: Int, height: Int, homeAlreadyConfirmed: Boolean = false): Boolean {
        if (!AutomationState.autoFeedEnabled) { reset(); return false }
        if (!AutomationState.enabled) { reset(); return false }
        val now=SystemClock.elapsedRealtime()
        val plane = image.planes.firstOrNull() ?: return false
        val buffer = plane.buffer
        fun rgb(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }

        // HomeScreenDetector uses the stable navigation/header icons. Do not additionally gate
        // feeding on stage artwork colours: bright stages such as Binary Load legitimately make
        // the old dark-deck/cyan ratios fail while the food bubble is plainly visible.
        if (!homeAlreadyConfirmed &&
            !de.robinthor.digiworldexplorer.automation.HomeScreenDetector.detect(width, height, ::rgb)) {
            stableFrames = 0; mainScreenFrames = 0; return false
        }
        mainScreenFrames++

        val frame = de.robinthor.digiworldexplorer.vision.PixelFrame(width, height) { x,y -> rgb(x,y) }
        val bubble = BondBubbleDetector.detect(frame)
        if (bubble == null) {
            // The animated bubble can disappear for individual capture frames. Once Home has
            // already been proven by BondRotation, retain nearby evidence briefly instead of
            // requiring an impossible uninterrupted run of detections.
            if (!homeAlreadyConfirmed || now - lastBubbleSeenAt > 1_500L) stableFrames = 0
            tapsLeft = 0
            return true
        }
        val (px,py) = de.robinthor.digiworldexplorer.vision.GameViewport.fit(width,height).pixel(bubble)
        val cx = px.toFloat(); val cy = py.toFloat()
        val recentConfirmedBubble = homeAlreadyConfirmed && now - lastBubbleSeenAt <= 1_500L
        if (recentConfirmedBubble ||
            (kotlin.math.abs(cx-lastX) < width*.04f && kotlin.math.abs(cy-lastY) < height*.035f)) stableFrames++
        else stableFrames=1
        lastX=cx; lastY=cy
        lastBubbleSeenAt=now
        if (progressSequence(now)) return true
        // The bubble can be exposed for only one sampled frame while a failed stage restarts.
        // In this branch both the caller and the Home detector have already established the
        // bounded COLLECT state, so the bubble detector itself is sufficient authorization.
        val requiredStableFrames = if (homeAlreadyConfirmed) 1 else 4
        if (stableFrames >= requiredStableFrames && mainScreenFrames >= requiredStableFrames && tapsLeft == 0 && now >= cooldownUntil) {
            tapsLeft = 1; tappingUntil = now + 3_000L; nextTapAt = now
        }
        return true
    }

    fun allowImmediateScan() {
        stableFrames = 0
        mainScreenFrames = 0
        cooldownUntil = 0L
        lastBubbleSeenAt = 0L
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
                    dispatchSafeRandomizedTap(lastX,lastY) { ok -> if (ok) lastCollectedAt = now }
                }
                if (tapsLeft==0) cooldownUntil=now+60_000L
            }
            return true
        }
        return false
    }

}

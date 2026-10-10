package de.robinthor.digiworldexplorer.feed

import android.graphics.Color
import de.robinthor.digiworldexplorer.capture.AnalysisImage as Image
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
    private var rotationTapAt = 0L
    private var rotationTapAttempts = 0
    private var rotationBubbleAbsentSince = 0L

    fun reset() { stableFrames = 0; mainScreenFrames = 0; tappingUntil = 0L; nextTapAt = 0L; tapsLeft = 0; cooldownUntil = 0L; lastBubbleSeenAt = 0L; lastCollectedAt = 0L; rotationTapAt = 0L; rotationTapAttempts = 0; rotationBubbleAbsentSince = 0L }
    fun collectedSince(since: Long): Boolean = lastCollectedAt >= since && since > 0L
    fun collectionSettledSince(since: Long, now: Long, settleMillis: Long = 1_500L): Boolean =
        collectedSince(since) && now - lastCollectedAt >= settleMillis

    fun analyze(image: Image, width: Int, height: Int, homeAlreadyConfirmed: Boolean = false, rotationOwned: Boolean = false): Boolean {
        if (!AutomationState.autoFeedEnabled && !rotationOwned) { reset(); return false }
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
        val reading = BondBubbleDetector.observe(frame)
        val bubble = reading?.center
        if (bubble == null) {
            // The animated bubble can disappear for individual capture frames. Once Home has
            // already been proven by BondRotation, retain nearby evidence briefly instead of
            // requiring an impossible uninterrupted run of detections.
            if (!homeAlreadyConfirmed || now - lastBubbleSeenAt > 1_500L) stableFrames = 0
            tapsLeft = 0
            if (rotationOwned && rotationTapAttempts > 0) {
                if (rotationBubbleAbsentSince == 0L) rotationBubbleAbsentSince = now
                if (now - rotationBubbleAbsentSince >= 700L && lastCollectedAt == 0L) {
                    lastCollectedAt = now
                    de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                        "BOND.BUBBLE_VERIFIED", "proved panel absent for 700ms after ${rotationTapAttempts} taps")
                    android.util.Log.i("DigiWorldBond", "collect verified: tapped bubble disappeared")
                }
            }
            return true
        }
        rotationBubbleAbsentSince = 0L
        val tapTarget = BondBubbleDetector.tapTarget(reading) ?: return true
        val (px,py) = reading.viewport.pixel(tapTarget)
        val cx = px.toFloat(); val cy = py.toFloat()
        val recentConfirmedBubble = homeAlreadyConfirmed && now - lastBubbleSeenAt <= 1_500L
        if (recentConfirmedBubble ||
            (kotlin.math.abs(cx-lastX) < width*.04f && kotlin.math.abs(cy-lastY) < height*.035f)) stableFrames++
        else stableFrames=1
        lastX=cx; lastY=cy
        lastBubbleSeenAt=now
        if (rotationOwned) {
            if (lastCollectedAt == 0L && rotationTapAttempts < 4 && now - rotationTapAt >= 750L) {
                DigiWorldAccessibilityService.instance?.let { service ->
                    rotationTapAt = now
                    rotationTapAttempts++
                    lastX = cx; lastY = cy
                    android.util.Log.i("DigiWorldBond", "collect detected bubble=$bubble target=$tapTarget attempt=$rotationTapAttempts")
                    de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                        "BOND.BUBBLE_TAP", "bubble=$bubble bounds=${reading.bounds} viewport=${reading.viewport} target=$tapTarget zone=upper-right-interior attempt=$rotationTapAttempts")
                    service.dispatchValidatedTap(cx, cy) { }
                }
            }
            return true
        }
        if (progressSequence(now, reading, cx, cy)) return true
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
        lastCollectedAt = 0L
        stableFrames = 0
        mainScreenFrames = 0
        cooldownUntil = 0L
        lastBubbleSeenAt = 0L
        rotationTapAt = 0L
        rotationTapAttempts = 0
        rotationBubbleAbsentSince = 0L
    }

    fun pauseForDigiWorld() {
        stableFrames = 0
        mainScreenFrames = 0
        tappingUntil = 0L
        nextTapAt = 0L
        tapsLeft = 0
    }
    private fun progressSequence(now:Long, reading:BondBubbleReading, x:Float, y:Float):Boolean {
        if (tapsLeft > 0) {
            if (now > tappingUntil) { tapsLeft=0; cooldownUntil=now+60_000L; return true }
            if (now >= nextTapAt) {
                tapsLeft--
                nextTapAt=now+SafeTapRandomizer.delay(520L,230L)
                DigiWorldAccessibilityService.instance?.apply {
                    updateStatusKeepingGrid(getString(R.string.overlay_auto_feed),true)
                    android.util.Log.i("DigiWorldBond", "collect current bubble=$x,$y")
                    de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                        "BOND.BUBBLE_TAP", "classic bubble=${reading.center} bounds=${reading.bounds} viewport=${reading.viewport} target=${BondBubbleDetector.tapTarget(reading)} zone=upper-right-interior")
                    // Android may report a cancelled gesture when the accepted tap immediately
                    // changes the game window. The positive bubble detection is the visual proof;
                    // record dispatch now so BondRotation advances after its one-second settle
                    // instead of re-tapping the same animated bubble.
                    lastCollectedAt = now
                    // Only this frame's measured interior target may be dispatched. No stored
                    // center fallback and no jitter toward the figure below the tiny bubble.
                    dispatchValidatedTap(x,y) { ok ->
                        if (!ok) android.util.Log.w("DigiWorldBond", "bubble gesture callback cancelled; keeping visual confirmation")
                    }
                }
                if (tapsLeft==0) cooldownUntil=now+60_000L
            }
            return true
        }
        return false
    }
}

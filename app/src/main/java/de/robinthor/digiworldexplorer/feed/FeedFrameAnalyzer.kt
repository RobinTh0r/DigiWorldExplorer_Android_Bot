package de.robinthor.digiworldexplorer.feed

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.input.SafeTapRandomizer
import de.robinthor.digiworldexplorer.strategy.AutomationState

/** Conservative detector for the small white food bubble on the main battle screen. */
object FeedFrameAnalyzer {
    private val bubbleTapHandler = Handler(Looper.getMainLooper())
    @Volatile private var bubbleBurstGeneration = 0L
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
    private var rotationFallbackAt = 0L
    private var rotationFallbackIndex = 0
    @Volatile private var lastCollectedAt = 0L
    private var fallbackFinishedAt = 0L
    private var rotationBurstRemaining = 0
    private var rotationBurstAt = 0L

    fun reset() { bubbleBurstGeneration++; stableFrames = 0; mainScreenFrames = 0; tappingUntil = 0L; nextTapAt = 0L; tapsLeft = 0; cooldownUntil = 0L; lastBubbleSeenAt = 0L; rotationFallbackAt = 0L; rotationFallbackIndex = 0; lastCollectedAt = 0L; rotationBurstRemaining = 0; rotationBurstAt = 0L }
    fun collectedSince(since: Long): Boolean = lastCollectedAt >= since && since > 0L
    fun fallbackSettledSince(since: Long, now: Long): Boolean =
        since > 0L && fallbackFinishedAt >= since && now - fallbackFinishedAt >= 1_500L
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

        if (rotationOwned && rotationBurstRemaining > 0 && now >= rotationBurstAt) {
            DigiWorldAccessibilityService.instance?.dispatchValidatedTap(lastX, lastY) { }
            rotationBurstRemaining--
            rotationBurstAt = now + 220L
        }

        val frame = de.robinthor.digiworldexplorer.vision.PixelFrame(width, height) { x,y -> rgb(x,y) }
        val bubble = BondBubbleDetector.detect(frame)
        if (bubble == null) {
            // The animated bubble can disappear for individual capture frames. Once Home has
            // already been proven by BondRotation, retain nearby evidence briefly instead of
            // requiring an impossible uninterrupted run of detections.
            if (!homeAlreadyConfirmed || now - lastBubbleSeenAt > 1_500L) stableFrames = 0
            tapsLeft = 0
            if (rotationOwned && homeAlreadyConfirmed && now >= rotationFallbackAt &&
                rotationFallbackIndex < ROTATION_FALLBACK_POINTS.size) {
                val service = DigiWorldAccessibilityService.instance ?: return true
                val target = ROTATION_FALLBACK_POINTS[rotationFallbackIndex++]
                rotationFallbackAt = now + 420L
                val (x, y) = de.robinthor.digiworldexplorer.vision.GameViewport.fit(width, height).pixel(target)
                android.util.Log.i("DigiWorldBond", "collect fallback ${rotationFallbackIndex}/${ROTATION_FALLBACK_POINTS.size} at=$x,$y")
                service.dispatchSafeRandomizedTap(x.toFloat(), y.toFloat()) { }
                if (rotationFallbackIndex == ROTATION_FALLBACK_POINTS.size) fallbackFinishedAt = now
                // A blind fallback never proves that a token was collected.
            }
            return true
        }
        val tapTarget = BondBubbleDetector.tapTarget(bubble) ?: return true
        val (px,py) = de.robinthor.digiworldexplorer.vision.GameViewport.fit(width,height).pixel(tapTarget)
        val cx = px.toFloat(); val cy = py.toFloat()
        val recentConfirmedBubble = homeAlreadyConfirmed && now - lastBubbleSeenAt <= 1_500L
        if (recentConfirmedBubble ||
            (kotlin.math.abs(cx-lastX) < width*.04f && kotlin.math.abs(cy-lastY) < height*.035f)) stableFrames++
        else stableFrames=1
        lastX=cx; lastY=cy
        lastBubbleSeenAt=now
        if (rotationOwned) {
            if (lastCollectedAt == 0L) {
                DigiWorldAccessibilityService.instance?.let { service ->
                    lastCollectedAt = now
                    lastX = cx; lastY = cy
                    rotationBurstRemaining = 0
                    android.util.Log.i("DigiWorldBond", "collect detected bubble=$bubble target=$tapTarget; burst")
                    dispatchBubbleBurst(service, bubble, tapTarget, width, height)
                }
            }
            return true
        }
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

    private fun dispatchBubbleBurst(
        service: DigiWorldAccessibilityService,
        bubble: de.robinthor.digiworldexplorer.vision.NormalizedPoint,
        figure: de.robinthor.digiworldexplorer.vision.NormalizedPoint,
        width: Int,
        height: Int,
    ) {
        val generation = ++bubbleBurstGeneration
        val viewport = de.robinthor.digiworldexplorer.vision.GameViewport.fit(width, height)
        // Some stages accept the figure below the speech bubble, others accept the bubble itself.
        // Cover both proven targets, with the figure first, instead of repeating one possibly
        // offset coordinate. The complete bounded burst finishes in about 1.2 seconds.
        val targets = listOf(figure, figure, bubble, figure, bubble).map { viewport.pixel(it) }
        targets.forEachIndexed { index, (x, y) ->
            bubbleTapHandler.postDelayed({
                if (generation != bubbleBurstGeneration || !AutomationState.enabled) return@postDelayed
                service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { }
            }, index * 280L)
        }
    }

    fun allowImmediateScan() {
        fallbackFinishedAt = 0L
        lastCollectedAt = 0L
        stableFrames = 0
        mainScreenFrames = 0
        cooldownUntil = 0L
        lastBubbleSeenAt = 0L
        rotationBurstRemaining = 0
        rotationBurstAt = 0L
        // Prefer visual evidence first. Only sweep the known stage bubble corridor when the
        // rotation has remained on its already verified Home boundary for a short grace period.
        rotationFallbackAt = SystemClock.elapsedRealtime() + 2_000L
        rotationFallbackIndex = 0
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
                    // Android may report a cancelled gesture when the accepted tap immediately
                    // changes the game window. The positive bubble detection is the visual proof;
                    // record dispatch now so BondRotation advances after its one-second settle
                    // instead of re-tapping the same animated bubble.
                    lastCollectedAt = now
                    dispatchSafeRandomizedTap(lastX,lastY) { ok ->
                        if (!ok) android.util.Log.w("DigiWorldBond", "bubble gesture callback cancelled; keeping visual confirmation")
                    }
                }
                if (tapsLeft==0) cooldownUntil=now+60_000L
            }
            return true
        }
        return false
    }

    private val ROTATION_FALLBACK_POINTS = listOf(
        // Centre-first, then the normal stage corridor. Repeated centre points intentionally
        // provide the requested classic friendship-style safety taps when animation hides the
        // bubble from one or two captured frames.
        de.robinthor.digiworldexplorer.vision.NormalizedPoint(.50, .40),
        de.robinthor.digiworldexplorer.vision.NormalizedPoint(.50, .40),
        de.robinthor.digiworldexplorer.vision.NormalizedPoint(.42, .40),
        de.robinthor.digiworldexplorer.vision.NormalizedPoint(.58, .40),
        de.robinthor.digiworldexplorer.vision.NormalizedPoint(.34, .40),
        de.robinthor.digiworldexplorer.vision.NormalizedPoint(.66, .40),
    )

}

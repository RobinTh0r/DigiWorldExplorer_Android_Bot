package de.robinthor.digiworldexplorer.feed

import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.automation.*
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.vision.*

object BondRotationAnalyzer {
    private var rotation = BondRotation()
    private var scanAt = 0L
    private var signature = ""
    private var matches = 0
    private var owns = false
    @Volatile private var fastPolling = false
    fun reset() { rotation = BondRotation(); scanAt = 0; signature = ""; matches = 0; owns = false; fastPolling = false }
    fun needsFastPolling() = fastPolling

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val forced = BondRotationRequest.active()
        val scheduled = DigiCopilotRequest.active() && AutomationState.autoBondRotationEnabled
        if (!AutomationState.enabled || (!forced && !scheduled)) {
            reset(); return false
        }
        val now = SystemClock.elapsedRealtime()
        fastPolling = rotation.fastBubblePolling(now)
        if (now < scanAt) return owns
        scanAt = now + if (fastPolling) 100 else 500
        val plane = image.planes.firstOrNull() ?: return owns
        val w = minOf(width, image.width); val h = minOf(height, image.height)
        val bytes = plane.buffer
        if (plane.pixelStride < 3 || w <= 0 || h <= 0 ||
            (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= bytes.limit()) return owns
        val frame = PixelFrame(w, h) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            (255 shl 24) or ((bytes.get(offset).toInt() and 255) shl 16) or
                ((bytes.get(offset + 1).toInt() and 255) shl 8) or (bytes.get(offset + 2).toInt() and 255)
        }
        // Use the same animation-tolerant Home decision as the Director/entry flow.
        val home = GameEntryDetector.detect(frame).screen == EntryScreen.HOME
        val grid = PartnerGridDetector.detect(frame)
        val key = "$home|${grid.page}|${grid.expanded}|${grid.raised}|${grid.selected}|${grid.canRaise}|${grid.confirmation}"
        if (key == signature) matches++ else { signature = key; matches = 1 }
        if (matches < 3) return owns
        val previous = rotation.step
        val bubble = home && BondBubbleDetector.detect(frame) != null
        val collectedSettled = FeedFrameAnalyzer.collectionSettledSince(rotation.collectStartedAt, now)
        val command = rotation.tick(home, grid, FeedFrameAnalyzer.isBusy(), now, bubble,
            forced || BondCycleTimer.canStartBond(now),
            collectedSettled)
        fastPolling = rotation.fastBubblePolling(now)
        if (matches == 3) android.util.Log.i(
            "DigiWorldBond",
            "probe step=${rotation.step} home=$home forced=$forced scheduled=$scheduled " +
                "feedBusy=${FeedFrameAnalyzer.isBusy()} gridPage=${grid.page} bubble=$bubble command=${command?.step}",
        )
        owns = rotation.ownsFrame()
        if (previous != BondStep.COLLECT && rotation.step == BondStep.COLLECT) FeedFrameAnalyzer.allowImmediateScan()
        if (rotation.step == BondStep.COLLECT) {
            // This Home boundary was confirmed by the rotation itself. Delegating here avoids the
            // stricter passive Home fingerprint rejecting battle-animation frames with a bubble.
            // Once a tap has been accepted, freeze collection until its one-second settle period
            // completes instead of tapping the still-visible animated bubble again.
            if (FeedFrameAnalyzer.collectedSince(rotation.collectStartedAt)) return true
            return FeedFrameAnalyzer.analyze(image, w, h, homeAlreadyConfirmed = true, rotationOwned = true)
        }
        if (previous != BondStep.REST && rotation.step == BondStep.REST) {
            BondRotationRequest.complete()
            BondCycleTimer.bondCompleted(now)
            if (AutomationState.autoFarmEnabled) BondFarmAnalyzer.requestVisit()
            else if (AutomationState.copilotRewardsEnabled) {
                BondCycleTimer.farmReturnedHome(now)
                HomeIdleRewardRequest.start()
            } else if (AutomationState.copilotDwsEnabled) {
                BondCycleTimer.farmReturnedHome(now)
                DwsExcursionRequest.start()
            } else {
                BondCycleTimer.farmReturnedHome(now)
                DigiCopilotRequest.stop("Selected modules complete")
            }
            android.util.Log.i("DigiWorldBond", "complete visited=${rotation.visited} restored=${rotation.original}; waiting for next bubble after farm")
        }
        val service = DigiWorldAccessibilityService.instance ?: return owns
        if (rotation.step == BondStep.PARK) {
            BondRotationRequest.park("Partner screen not confirmed")
            service.showStatusOnly("Bond rotation paused: partner not confirmed")
            return true
        }
        if (command == null) return owns
        val target = when (command.step) {
            BondStep.OPEN -> NormalizedPoint(.254, .956)
            BondStep.EXPAND -> NormalizedPoint(.823, .818)
            BondStep.SELECT -> grid.cells.getOrNull(command.cell ?: -1)
            BondStep.RAISE -> grid.raiseTarget
            BondStep.CONFIRM -> NormalizedPoint(.634, .59)
            BondStep.HOME -> NormalizedPoint(.5, .947)
            else -> null
        } ?: return owns
        FeedFrameAnalyzer.pauseForDigiWorld()
        val tapViewport = if (command.step == BondStep.OPEN) {
            HomeScreenDetector.viewport(w, h, frame::argbAt)
                ?: if (previous == BondStep.COLLECT && collectedSettled) GameViewport.fit(w, h) else return owns
        } else GameViewport.fit(w,h)
        val (x,y) = tapViewport.pixel(target)
        service.showStatusOnly("Bond ${rotation.visited}/15: ${command.step.name.lowercase()}")
        android.util.Log.i("DigiWorldBond", "step=${command.step} cell=${command.cell} original=${rotation.original} visited=${rotation.visited}")
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { success ->
            // Gesture callbacks can be cancelled by the game's immediate window transition even
            // when the tap was accepted. Only subsequent visual state or the bounded deadline may
            // confirm/fail the step; a callback alone must not destroy the whole rotation.
            if (!success) android.util.Log.w("DigiWorldBond", "gesture callback cancelled for ${command.step}; awaiting visual proof")
        }
        matches = 0
        return true
    }
}

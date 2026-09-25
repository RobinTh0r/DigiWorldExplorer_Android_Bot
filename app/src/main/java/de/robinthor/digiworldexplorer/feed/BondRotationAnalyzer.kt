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
    fun reset() { rotation = BondRotation(); scanAt = 0; signature = ""; matches = 0; owns = false }

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.enabled || !AutomationState.autoFeedEnabled || AutomationState.mode != AutomationMode.FULL_AUTOPILOT) {
            reset(); return false
        }
        val now = SystemClock.elapsedRealtime()
        if (now < scanAt) return owns
        scanAt = now + 500
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
        val home = HomeScreenDetector.detect(w, h, frame::argbAt)
        val grid = PartnerGridDetector.detect(frame)
        val key = "$home|${grid.page}|${grid.expanded}|${grid.raised}|${grid.selected}|${grid.canRaise}|${grid.confirmation}"
        if (key == signature) matches++ else { signature = key; matches = 1 }
        if (matches < 3) return owns
        val previous = rotation.step
        val bubble = home && BondBubbleDetector.detect(frame) != null
        val command = rotation.tick(home, grid, FeedFrameAnalyzer.isBusy(), now, bubble,
            BondCycleTimer.canStartBond(now))
        owns = rotation.ownsFrame()
        if (previous != BondStep.COLLECT && rotation.step == BondStep.COLLECT) FeedFrameAnalyzer.allowImmediateScan()
        if (previous != BondStep.REST && rotation.step == BondStep.REST) {
            BondCycleTimer.bondCompleted()
            if (AutomationState.autoFarmEnabled) BondFarmAnalyzer.requestVisit()
            android.util.Log.i("DigiWorldBond", "complete visited=${rotation.visited} restored=${rotation.original}; waiting for next bubble after farm")
        }
        val service = DigiWorldAccessibilityService.instance ?: return owns
        if (rotation.step == BondStep.PARK) {
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
        val (x,y) = GameViewport.fit(w,h).pixel(target)
        service.showStatusOnly("Bond ${rotation.visited}/15: ${command.step.name.lowercase()}")
        android.util.Log.i("DigiWorldBond", "step=${command.step} cell=${command.cell} original=${rotation.original} visited=${rotation.visited}")
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { success -> if (!success) rotation.cancel() }
        matches = 0
        return true
    }
}

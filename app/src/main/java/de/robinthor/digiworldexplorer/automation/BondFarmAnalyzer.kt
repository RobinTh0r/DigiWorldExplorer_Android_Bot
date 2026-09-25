package de.robinthor.digiworldexplorer.automation

import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.farm.*
import de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.vision.*

/** Owns navigation frames exclusively; Farm and Bond retain their existing action controllers. */
object BondFarmAnalyzer {
    private var cycle = BondFarmCycle()
    private var scanAt = 0L
    private var candidate = CycleScreen.OTHER
    private var matches = 0
    private var owns = false
    private var blocked = false
    var screen = ObservedScreen.UNKNOWN
        private set

    fun requestVisit() { cycle.requestVisit() }

    fun reset() {
        cycle = BondFarmCycle(); scanAt = 0; matches = 0; owns = false; blocked = false
        candidate = CycleScreen.OTHER; screen = ObservedScreen.UNKNOWN
    }

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.active()) return false
        if (!AutomationState.enabled || !AutomationState.autoFarmEnabled ||
            !AutomationState.autoFeedEnabled || AutomationState.mode != AutomationMode.FULL_AUTOPILOT) {
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
        val viewport = GameViewport.fit(w, h)
        val explore = ExploreMenuDetector.detect(frame, viewport)
        val known = KnownPageDetector.detect(frame, viewport)
        val farm = if (!explore.menu && known == ObservedScreen.UNKNOWN)
            FarmHarvestDetector.detect(frame, viewport) else FarmHarvestDetection(false, emptyList())
        val seen = when {
            known != ObservedScreen.UNKNOWN -> CycleScreen.OTHER
            farm.field -> CycleScreen.FIELD
            explore.meatFieldTarget != null -> CycleScreen.EXPLORE
            HomeScreenDetector.detect(w, h, frame::argbAt) -> CycleScreen.HOME
            else -> CycleScreen.OTHER
        }
        screen = when (seen) {
            CycleScreen.FIELD -> ObservedScreen.MEAT_FIELD
            CycleScreen.EXPLORE -> ObservedScreen.EXPLORE_MENU
            CycleScreen.HOME -> ObservedScreen.HOME
            else -> ObservedScreen.UNKNOWN
        }
        if (seen == candidate) matches++ else { candidate = seen; matches = 1 }
        if (matches < 2) return owns
        val step = if (blocked) CycleStep.PARK else cycle.tick(seen,
            FarmHarvestAnalyzer.visitComplete, FeedFrameAnalyzer.isBusy(), now)
        if (cycle.consumeReturnedHome()) BondCycleTimer.farmReturnedHome(now)
        if (step == CycleStep.PARK && seen == CycleScreen.FIELD && BondCycleTimer.awaitingFarm()) {
            blocked = false
            return false
        }
        owns = step !in setOf(CycleStep.BOND, CycleStep.FARM)
        if (!owns) return false
        val service = DigiWorldAccessibilityService.instance ?: return true
        if (step == CycleStep.PARK) {
            service.showStatusOnly("Farm route paused: screen not confirmed")
            return true
        }
        val target = when (step) {
            CycleStep.OPEN_EXPLORE -> NormalizedPoint(.75, .955)
            CycleStep.OPEN_FIELD -> explore.meatFieldTarget
            CycleStep.CLOSE_FIELD -> NormalizedPoint(.835, .955)
            CycleStep.HOME -> NormalizedPoint(.5, .947)
            else -> null
        } ?: return true
        FeedFrameAnalyzer.pauseForDigiWorld()
        if (step == CycleStep.OPEN_FIELD) FarmHarvestAnalyzer.reset()
        service.showStatusOnly("Bond / Farm: ${step.name.lowercase().replace('_', ' ')}")
        val (x, y) = viewport.pixel(target)
        android.util.Log.i("DigiWorldCycle", "step=$step screen=$seen target=$target")
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { success -> if (!success) blocked = true }
        matches = 0
        return true
    }
}

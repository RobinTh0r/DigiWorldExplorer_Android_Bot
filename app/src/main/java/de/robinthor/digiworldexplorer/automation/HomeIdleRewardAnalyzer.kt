package de.robinthor.digiworldexplorer.automation

import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.vision.PixelFrame

object HomeIdleRewardRequest {
    enum class Phase { IDLE, WAIT_HOME, WAIT_DIALOG, CLAIMING, COMPLETE, PARKED }
    @Volatile var phase = Phase.IDLE
        private set
    @Volatile var reason = ""
        private set
    @Synchronized fun start() { phase = Phase.WAIT_HOME; reason = "Waiting for Home reward chest" }
    @Synchronized fun waitingDialog() { phase = Phase.WAIT_DIALOG; reason = "Opening idle rewards" }
    @Synchronized fun dialogSeen() { phase = Phase.CLAIMING; reason = "Claiming idle rewards" }
    @Synchronized fun complete() { phase = Phase.COMPLETE; reason = "Idle rewards checked" }
    @Synchronized fun park(why: String) { phase = Phase.PARKED; reason = why }
    @Synchronized fun reset() { phase = Phase.IDLE; reason = "" }
    fun active() = phase in setOf(Phase.WAIT_HOME, Phase.WAIT_DIALOG, Phase.CLAIMING)
}

/** Exclusive bridge from the Home chest into the existing, tested entry reward controller. */
object HomeIdleRewardAnalyzer {
    private var stable = 0
    private var absentStable = 0
    private var deadline = 0L
    private var chestTapAt = 0L
    private var chestTapRetries = 0

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!HomeIdleRewardRequest.active()) return false
        val plane = image.planes.firstOrNull() ?: return true
        val w = minOf(width, image.width); val h = minOf(height, image.height)
        if (plane.pixelStride < 3 || w <= 0 || h <= 0) return true
        val buffer = plane.buffer
        val frame = PixelFrame(w, h) { x, y ->
            val i = y * plane.rowStride + x * plane.pixelStride
            (255 shl 24) or ((buffer.get(i).toInt() and 255) shl 16) or
                ((buffer.get(i + 1).toInt() and 255) shl 8) or (buffer.get(i + 2).toInt() and 255)
        }
        val now = SystemClock.elapsedRealtime()
        val entry = GameEntryDetector.detect(frame)
        if (entry.screen in setOf(EntryScreen.IDLE_CLAIM, EntryScreen.IDLE_EMPTY, EntryScreen.RESULT)) {
            HomeIdleRewardRequest.dialogSeen()
            deadline = now + 45_000L
            return GameEntryAnalyzer.analyze(image, w, h)
        }
        if (HomeIdleRewardRequest.phase == HomeIdleRewardRequest.Phase.CLAIMING && entry.screen == EntryScreen.HOME) {
            HomeIdleRewardRequest.complete()
            if (de.robinthor.digiworldexplorer.strategy.AutomationState.copilotDwsEnabled) DwsExcursionRequest.start()
            stable = 0
            DigiWorldAccessibilityService.instance?.showStatusOnly("Idle rewards checked — Home", sourceScreen = ObservedScreen.HOME)
            return false
        }
        if (HomeIdleRewardRequest.phase == HomeIdleRewardRequest.Phase.WAIT_HOME) {
            if (deadline == 0L) deadline = now + 20_000L
            // The request can only be created by BondFarm after a stable Home confirmation.
            // Preserve that bounded evidence across the next animated Home frames.
            val reading = HomeIdleRewardDetector.detect(frame, homeAlreadyConfirmed = true)
            if (!reading.available) {
                stable = 0
                absentStable++
                // This phase is created only at a freshly verified Home boundary. A stable
                // absence therefore means the animated box currently has nothing to claim.
                if (absentStable >= 3) {
                    HomeIdleRewardRequest.complete()
                    if (de.robinthor.digiworldexplorer.strategy.AutomationState.copilotDwsEnabled) DwsExcursionRequest.start()
                }
                return HomeIdleRewardRequest.active()
            }
            absentStable = 0
            stable++
            if (stable < 2) return true
            val viewport = HomeScreenDetector.viewport(w, h, frame::argbAt) ?: return true
            val target = reading.target ?: return true
            val (x, y) = viewport.pixel(target)
            HomeIdleRewardRequest.waitingDialog()
            deadline = now + 20_000L
            chestTapAt = now
            chestTapRetries = 0
            DigiWorldAccessibilityService.instance?.dispatchValidatedTap(x.toFloat(), y.toFloat()) { ok ->
                if (!ok) android.util.Log.w("DigiWorldRewards", "chest gesture callback cancelled; awaiting visual proof")
            }
            return true
        }
        if (HomeIdleRewardRequest.phase == HomeIdleRewardRequest.Phase.WAIT_DIALOG &&
            entry.screen == EntryScreen.HOME && now - chestTapAt >= 1_500L && chestTapRetries < 2) {
            val reading = HomeIdleRewardDetector.detect(frame, homeAlreadyConfirmed = true)
            val viewport = HomeScreenDetector.viewport(w, h, frame::argbAt) ?: de.robinthor.digiworldexplorer.vision.GameViewport.fit(w, h)
            val target = reading.target
            if (reading.available && target != null) {
                val (x, y) = viewport.pixel(target)
                chestTapAt = now
                chestTapRetries++
                DigiWorldAccessibilityService.instance?.dispatchValidatedTap(x.toFloat(), y.toFloat()) { }
            }
        }
        if (deadline > 0 && now >= deadline) HomeIdleRewardRequest.park("Idle reward screen not confirmed")
        return true
    }

    fun reset() { stable = 0; absentStable = 0; deadline = 0; chestTapAt = 0; chestTapRetries = 0; HomeIdleRewardRequest.reset() }
}

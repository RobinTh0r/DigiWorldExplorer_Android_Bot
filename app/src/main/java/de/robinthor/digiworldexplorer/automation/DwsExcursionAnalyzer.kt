package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.capture.AnalysisImage as Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.CaptureFrameAnalyzer
import de.robinthor.digiworldexplorer.farm.ExploreMenuDetector
import de.robinthor.digiworldexplorer.farm.FarmHarvestDetector
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame

object DwsExcursionRequest {
    const val MAX_RUN_MILLIS = 5 * 60_000L
    enum class Phase { IDLE, OPEN_EXPLORE, OPEN_DWS, RUNNING, RETURNING, COMPLETE, PARKED }
    @Volatile var phase = Phase.IDLE
        private set
    @Volatile var reason = ""
        private set
    @Volatile var runStartedAt = 0L
        private set
    @Synchronized fun start() { phase = Phase.OPEN_EXPLORE; reason = "Opening Digital World Search"; runStartedAt = 0 }
    @Synchronized fun openingDws() { phase = Phase.OPEN_DWS; reason = "Opening Digital World Search" }
    @Synchronized fun entered(now: Long) {
        phase = Phase.RUNNING; reason = "Digital World Search"
        if (runStartedAt == 0L) runStartedAt = now
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DWS.ENTER", "startedElapsed=$runStartedAt")
    }
    @Synchronized fun requestReturn(why: String) {
        if (active()) {
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("DWS.RETURN", "from=$phase reason=$why startedElapsed=$runStartedAt")
            phase = Phase.RETURNING; reason = why
        }
    }
    @Synchronized fun complete() { phase = Phase.COMPLETE; reason = "DWS complete — Home"; runStartedAt = 0 }
    @Synchronized fun park(why: String) { phase = Phase.PARKED; reason = why }
    @Synchronized fun reset() { phase = Phase.IDLE; reason = ""; runStartedAt = 0 }
    fun active() = phase in setOf(Phase.OPEN_EXPLORE, Phase.OPEN_DWS, Phase.RUNNING, Phase.RETURNING)
    fun running() = phase == Phase.RUNNING
}

/** Bounded optional DWS visit. It never treats Dash=0 as proof that normal steps are empty. */
object DwsExcursionAnalyzer {
    private var candidate = ""
    private var matches = 0
    private var deadline = 0L

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!DwsExcursionRequest.active()) return false
        if (!DigiCopilotRequest.active()) { reset(); return false }
        val plane = image.planes.firstOrNull() ?: return true
        val w = minOf(width, image.width); val h = minOf(height, image.height)
        // Snapshot plane metadata while the Image is unquestionably open. Reading rowStride or
        // pixelStride lazily from PixelFrame's callback can race ImageReader closing the frame.
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val bytes = plane.buffer
        if (pixelStride < 3 || w <= 0 || h <= 0 ||
            (h - 1L) * rowStride + (w - 1L) * pixelStride + 2 >= bytes.limit()) return true
        val frame = PixelFrame(w, h) { x, y ->
            val i = y * rowStride + x * pixelStride
            (255 shl 24) or ((bytes.get(i).toInt() and 255) shl 16) or
                ((bytes.get(i + 1).toInt() and 255) shl 8) or (bytes.get(i + 2).toInt() and 255)
        }
        val now = SystemClock.elapsedRealtime()
        val service = DigiWorldAccessibilityService.instance ?: return true
        val viewport = GameViewport.fit(w, h)
        val homeViewport = HomeScreenDetector.viewport(w, h, frame::argbAt)
        val explore = ExploreMenuDetector.detect(frame, viewport)
        val knownPage = KnownPageDetector.detect(frame)
        var grid = CaptureFrameAnalyzer.isCalibrated

        when (DwsExcursionRequest.phase) {
            DwsExcursionRequest.Phase.OPEN_EXPLORE -> {
                if (!stable("home:${homeViewport != null}")) return true
                if (homeViewport == null) return waitOrPark(now, service, "Home not confirmed for DWS")
                deadline = now + 30_000L
                tap(service, homeViewport, NormalizedPoint(.75, .955), "Opening Explore")
                DwsExcursionRequest.openingDws()
                return true
            }
            DwsExcursionRequest.Phase.OPEN_DWS -> {
                if (!grid && !explore.menu) {
                    grid = CaptureFrameAnalyzer.analyze(service, image, w, h)?.detected == true
                }
                if (grid) {
                    DwsExcursionRequest.entered(now)
                    deadline = 0L
                    return false
                }
                if (!stable("explore:${explore.menu}:${explore.worldSearchTarget != null}")) return true
                // Both the page and card anchor must be present; page colour alone never
                // authorizes a blind card tap while an animation or overlay hides the target.
                if(!explore.menu)return waitOrPark(now,service,"Explore not confirmed for DWS")
                val target = explore.worldSearchTarget ?: return waitOrPark(now,service,"DWS card not confirmed")
                deadline = now + 30_000L
                tap(service, viewport, target, "Opening Digital World Search")
                return true
            }
            DwsExcursionRequest.Phase.RUNNING -> {
                if (AutomationState.autoBondRotationEnabled && BondCycleTimer.remainingMillis(now) == 0L)
                    DwsExcursionRequest.requestReturn("Bond timer ready")
                else if (now - DwsExcursionRequest.runStartedAt >= DwsExcursionRequest.MAX_RUN_MILLIS)
                    DwsExcursionRequest.requestReturn("Five-minute DWS limit reached")
                else return false // calibrated grid keeps ownership and moves normally
            }
            DwsExcursionRequest.Phase.RETURNING -> Unit
            else -> return false
        }

        if (homeViewport != null && stable("return-home")) {
            DwsExcursionRequest.complete(); AutoMoveController.reset(); CaptureFrameAnalyzer.resetCalibration()
            service.showStatusOnly("DWS complete — waiting for Bond timer", sourceScreen = ObservedScreen.HOME)
            return false
        }
        if (explore.menu && stable("return-explore")) {
            tap(service, viewport, NormalizedPoint(.5, .947), "Returning Home")
            deadline = now + 30_000L
            return true
        }
        if (grid) {
            AutoMoveController.reset(); CaptureFrameAnalyzer.resetCalibration()
            // Android Back may return to the previously opened Partner page. The in-game X is
            // the deterministic DWS → Explore transition across BlueStacks and phones.
            val close=FarmHarvestDetector.closeTarget(frame,viewport)
                ?: return waitOrPark(now,service,"DWS close control not confirmed")
            tap(service, viewport, close, "Leaving Digital World Search")
            deadline = now + 30_000L
            return true
        }
        if (knownPage != ObservedScreen.UNKNOWN && stable("return-known:$knownPage")) {
            tap(service, viewport, NormalizedPoint(.5, .947), "Recovering Home")
            deadline = now + 30_000L
            return true
        }
        return waitOrPark(now, service, "DWS return screen not confirmed")
    }

    private fun waitOrPark(now: Long, service: DigiWorldAccessibilityService, reason: String): Boolean {
        if (deadline == 0L) deadline = now + 30_000L
        if (now >= deadline) { DwsExcursionRequest.park(reason); service.showStatusOnly(reason) }
        return true
    }
    private fun stable(value: String): Boolean {
        if (candidate == value) matches++ else { candidate = value; matches = 1 }
        return matches >= 2
    }
    private fun tap(service: DigiWorldAccessibilityService, viewport: GameViewport, point: NormalizedPoint, label: String) {
        val (x, y) = viewport.pixel(point); service.showStatusOnly(label)
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { ok -> if (!ok) DwsExcursionRequest.park("$label failed") }
        candidate = ""; matches = 0
    }
    fun reset() { candidate = ""; matches = 0; deadline = 0; DwsExcursionRequest.reset() }
}

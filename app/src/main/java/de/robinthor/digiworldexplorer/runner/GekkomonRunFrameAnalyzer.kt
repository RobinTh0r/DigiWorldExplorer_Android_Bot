package de.robinthor.digiworldexplorer.runner

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object GekkomonRunFrameAnalyzer {
    private const val ACTION_COOLDOWN = 600L
    private const val MIN_ACTION_CONFIDENCE = .75
    private const val REQUIRED_ACTIVE_FRAMES = 3
    private var lastAction = RunnerAction.NONE
    private var stableFrames = 0
    private var activeFrames = 0
    private var lastTap = 0L
    private var pending = false

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.autoRunnerEnabled) { reset(); return false }
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        val w = minOf(width, image.width)
        val h = minOf(height, image.height)
        if (w <= 0 || h <= 0 || (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= buffer.limit()) {
            reset()
            return false
        }
        fun pixel(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val detection = GekkomonRunScreenDetector.detect(w, h, ::pixel)
        if (!detection.active || detection.confidence < MIN_ACTION_CONFIDENCE) {
            lastAction = RunnerAction.NONE
            stableFrames = 0
            activeFrames = 0
            return false
        }
        activeFrames++
        AutoMoveController.pauseForPurchaseScreen()
        val service = DigiWorldAccessibilityService.instance
        service?.showStatusOnly(service.getString(R.string.overlay_auto_runner))
        if (detection.action == RunnerAction.NONE) { lastAction = RunnerAction.NONE; stableFrames = 0; return true }
        if (detection.action == lastAction) stableFrames++ else { lastAction = detection.action; stableFrames = 1 }
        val now = SystemClock.elapsedRealtime()
        if (!pending && activeFrames >= REQUIRED_ACTIVE_FRAMES && stableFrames >= 2 &&
            AutomationState.enabled && now - lastTap >= ACTION_COOLDOWN
        ) {
            pending = true
            lastTap = now
            val x = if (detection.action == RunnerAction.JUMP) .26f else .692f
            service?.dispatchNormalizedTap(x, .897f) { ok ->
                pending = false
                Log.i("DigiWorldRunner", "${detection.action} tap=$ok obstacleX=${detection.obstacleX} confidence=${detection.confidence}")
            } ?: run { pending = false }
            stableFrames = 0
        }
        return true
    }

    fun reset() { lastAction = RunnerAction.NONE; stableFrames = 0; activeFrames = 0; lastTap = 0L; pending = false }
}

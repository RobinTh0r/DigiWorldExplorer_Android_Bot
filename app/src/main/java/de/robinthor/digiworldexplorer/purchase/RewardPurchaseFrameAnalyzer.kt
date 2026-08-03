package de.robinthor.digiworldexplorer.purchase

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object RewardPurchaseFrameAnalyzer {
    private const val TAP_INTERVAL = 200L
    @Volatile private var pending = false
    private var lastTap = 0L
    private var wasRecognized = false

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride < 3) return false
        val detection = RewardPurchaseDetector.detect(width, height) { x, y ->
            val offset = y * rowStride + x * pixelStride
            Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        if (!detection.recognized) {
            if (wasRecognized) Log.i("DigiWorldPurchase", "reward screen closed or covered; tapping stopped")
            wasRecognized = false
            return false
        }
        wasRecognized = true
        AutoMoveController.pauseForPurchaseScreen()
        if (!AutomationState.enabled || !detection.affordable) {
            if (!detection.affordable) Log.i("DigiWorldPurchase", "cost 30 is red; no purchase")
            return true
        }
        val now = SystemClock.elapsedRealtime()
        if (pending || now - lastTap < TAP_INTERVAL) return true
        val service = DigiWorldAccessibilityService.instance ?: return true
        pending = true
        lastTap = now
        service.dispatchValidatedTap(detection.tapX, detection.tapY) { ok ->
            pending = false
            Log.i("DigiWorldPurchase", "summon tap=$ok red=${detection.redCostRatio}")
        }
        return true
    }

    fun reset() { pending = false; lastTap = 0L; wasRecognized = false }
}

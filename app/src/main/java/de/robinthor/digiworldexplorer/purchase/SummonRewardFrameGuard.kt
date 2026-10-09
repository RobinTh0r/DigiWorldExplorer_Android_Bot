package de.robinthor.digiworldexplorer.purchase

import android.graphics.Color
import de.robinthor.digiworldexplorer.capture.AnalysisImage as Image
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutoMoveController

/** Claims the card-result page before other automation modules can misread its blue panels. */
object SummonRewardFrameGuard {
    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        val state = SummonRewardScreenDetector.detectState(width, height) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255,
                buffer.get(offset + 2).toInt() and 255)
        }
        // Only the unfinished reveal blocks all other analyzers. A stable result is handled by
        // RewardPurchaseFrameAnalyzer, which may start the next user-enabled classic summon.
        val blocking = state == SummonRewardState.REVEAL
        if (blocking) {
            AutoMoveController.pauseForPurchaseScreen()
            DigiWorldAccessibilityService.instance?.showStatusOnly("Summon reveal running")
        }
        return blocking
    }
    fun reset() = Unit
}

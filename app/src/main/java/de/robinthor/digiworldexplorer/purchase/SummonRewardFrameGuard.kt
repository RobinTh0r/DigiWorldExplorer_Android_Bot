package de.robinthor.digiworldexplorer.purchase

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutoMoveController

/** Claims the card-result page before other automation modules can misread its blue panels. */
object SummonRewardFrameGuard {
    private var lastSeenAt = 0L
    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        val found = SummonRewardScreenDetector.detect(width, height) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255,
                buffer.get(offset + 2).toInt() and 255)
        }
        val now = SystemClock.elapsedRealtime()
        if (found) lastSeenAt = now
        val blocking = found || (lastSeenAt > 0L && now - lastSeenAt < 8_000L)
        if (blocking) {
            RewardPurchaseFrameAnalyzer.onRewardScreen()
            AutoMoveController.pauseForPurchaseScreen()
            DigiWorldAccessibilityService.instance?.showStatusOnly(if (found) "Summon reward: close manually" else "Waiting for summon reveal")
        }
        return blocking
    }
    fun reset() { lastSeenAt = 0L }
}

package de.robinthor.digiworldexplorer.purchase

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.input.SafeTapRandomizer
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object RewardPurchaseFrameAnalyzer {
    private const val TAP_INTERVAL = 200L
    private const val SEQUENCE_TIMEOUT = 5_000L
    private const val CREST_REVEAL_TIMEOUT = 10_000L
    @Volatile private var pending = false
    private var lastTap = 0L
    private var nextTapInterval = TAP_INTERVAL
    private var sequenceUntil = 0L
    private var sequenceTapX = 0f
    private var sequenceTapY = 0f

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride < 3) return false
        val argbAt = { x: Int, y: Int ->
            val offset = y * rowStride + x * pixelStride
            Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val detection = RewardPurchaseDetector.detect(width, height, argbAt)
        val now = SystemClock.elapsedRealtime()

        // Crest summons insert a second confirmation dialog between the regular yellow buy button
        // and the reveal sequence. Only accept it while a summon initiated by this analyzer is
        // active, so other blue/yellow dialogs can never trigger the confirmation tap.
        if (sequenceUntil > now) {
            val crestConfirmation = RewardPurchaseDetector.detectCrestConfirmation(width, height, argbAt)
            if (crestConfirmation.recognized) {
                AutoMoveController.pauseForPurchaseScreen()
                DigiWorldAccessibilityService.instance?.let { service ->
                    service.showStatusOnly(service.getString(R.string.overlay_auto_purchase))
                    if (AutomationState.enabled && AutomationState.autoPurchaseEnabled) {
                        sequenceUntil = now + CREST_REVEAL_TIMEOUT
                        tryTap(service, crestConfirmation.tapX, crestConfirmation.tapY, now)
                    }
                }
                return true
            }
        }

        if (!detection.recognized) {
            if (sequenceUntil > now && AutomationState.enabled && AutomationState.autoPurchaseEnabled) {
                AutoMoveController.pauseForPurchaseScreen()
                DigiWorldAccessibilityService.instance?.let { service ->
                    service.showStatusOnly(service.getString(R.string.overlay_auto_purchase))
                    tryTap(service, sequenceTapX, sequenceTapY, now)
                }
                return true
            }
            if (sequenceUntil != 0L) Log.i("DigiWorldPurchase", "summon sequence timed out or was stopped")
            sequenceUntil = 0L
            return false
        }
        AutoMoveController.pauseForPurchaseScreen()
        DigiWorldAccessibilityService.instance?.let { service ->
            service.showStatusOnly(service.getString(if (AutomationState.autoPurchaseEnabled) R.string.overlay_auto_purchase else R.string.overlay_purchase_disabled))
        }
        if (!AutomationState.enabled || !AutomationState.autoPurchaseEnabled || !detection.affordable) {
            sequenceUntil = 0L
            if (!detection.affordable) Log.i("DigiWorldPurchase", "summon cost is red; summon sequence stopped")
            return true
        }
        sequenceTapX = detection.tapX
        sequenceTapY = if (AutomationState.summonTouchCorrection) {
            (detection.tapY + height * .035f).coerceAtMost(height * .99f)
        } else detection.tapY
        sequenceUntil = now + SEQUENCE_TIMEOUT
        DigiWorldAccessibilityService.instance?.let { tryTap(it, sequenceTapX, sequenceTapY, now) }
        return true
    }

    private fun tryTap(service: DigiWorldAccessibilityService, x: Float, y: Float, now: Long) {
        if (pending || now - lastTap < nextTapInterval) return
        pending = true
        lastTap = now
        nextTapInterval = SafeTapRandomizer.delay(TAP_INTERVAL, 20L)
        service.dispatchSafeRandomizedTap(x, y) { ok ->
            pending = false
            Log.i("DigiWorldPurchase", "summon/advance tap=$ok")
        }
    }

    fun reset() { pending = false; lastTap = 0L; nextTapInterval = TAP_INTERVAL; sequenceUntil = 0L }
}
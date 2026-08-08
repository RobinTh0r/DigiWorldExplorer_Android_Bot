package de.robinthor.digiworldexplorer.network

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object NetworkDefenseFrameAnalyzer {
    private const val TAP_INTERVAL = 250L
    private const val SESSION_TIMEOUT = 5 * 60_000L
    private var sessionActive = false
    private var pending = false
    private var lastTap = 0L
    private var sessionStarted = 0L
    private var lastScreen = NetworkDefenseScreen.NONE
    private var startTaps = 0

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        fun pixel(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val detection = NetworkDefenseScreenDetector.detect(width, height, ::pixel)
        val now = SystemClock.elapsedRealtime()
        if (detection.screen != NetworkDefenseScreen.NONE) AutoMoveController.pauseForPurchaseScreen()

        if (!AutomationState.autoNetworkDefenseEnabled) {
            sessionActive = false
            if (detection.screen != NetworkDefenseScreen.NONE) {
                DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(R.string.overlay_network_disabled)) }
                return true
            }
            return false
        }

        if (detection.screen == NetworkDefenseScreen.START) {
            if (!sessionActive) sessionStarted = now
            sessionActive = true
            if (lastScreen != detection.screen) startTaps = 0
            lastScreen = detection.screen
            showStatus(R.string.overlay_network_start)
            if (AutomationState.enabled && startTaps < 2) tryTap(detection, now) { startTaps++ }
            return true
        }

        if (detection.screen == NetworkDefenseScreen.DIABOROMON) {
            if (!sessionActive) sessionStarted = now
            sessionActive = true
            lastScreen = detection.screen
            showStatus(R.string.overlay_network_give_up)
            if (AutomationState.enabled) tryTap(detection, now)
            return true
        }

        if (!sessionActive) return false
        if (now - sessionStarted >= SESSION_TIMEOUT) {
            AutomationState.autoNetworkDefenseEnabled = false
            sessionActive = false
            showStatus(R.string.overlay_network_timeout)
            Log.w("DigiWorldNetwork", "Network Defense Ops stopped after session timeout")
            return true
        }
        lastScreen = NetworkDefenseScreen.NONE
        showStatus(R.string.overlay_network_waiting)
        return true
    }

    private fun tryTap(detection: NetworkDefenseDetection, now: Long, afterDispatch: () -> Unit = {}) {
        if (pending || now - lastTap < TAP_INTERVAL) return
        val service = DigiWorldAccessibilityService.instance ?: return
        pending = true
        lastTap = now
        afterDispatch()
        service.dispatchValidatedTap(detection.tapX, detection.tapY) { ok ->
            pending = false
            Log.i("DigiWorldNetwork", "${detection.screen} tap=$ok confidence=${detection.confidence}")
        }
    }

    private fun showStatus(stringId: Int) {
        DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(stringId)) }
    }

    fun reset() {
        sessionActive = false
        pending = false
        lastTap = 0L
        sessionStarted = 0L
        lastScreen = NetworkDefenseScreen.NONE
        startTaps = 0
    }
}
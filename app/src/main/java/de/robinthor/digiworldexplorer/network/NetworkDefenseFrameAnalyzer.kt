package de.robinthor.digiworldexplorer.network

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.input.SafeTapRandomizer
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object NetworkDefenseFrameAnalyzer {
    // Network Defense dialogs can take noticeably longer to settle on BlueStacks. A confirmed
    // screen is therefore retried deliberately instead of firing a short two-tap burst.
    private const val START_SETTLE_TIME = 650L
    private const val START_RETRY_INTERVAL = 2_500L
    private const val BOSS_RETRY_INTERVAL = 650L
    private const val PENDING_GESTURE_TIMEOUT = 1_800L
    private const val SESSION_TIMEOUT = 5 * 60_000L
    private const val SESSION_EVIDENCE_GRACE = 20_000L
    private const val BOSS_STATUS_HOLD = 5_000L
    private var sessionActive = false
    private var pending = false
    private var pendingSince = 0L
    private var lastTap = 0L
    private var nextTapInterval = START_RETRY_INTERVAL
    private var sessionStarted = 0L
    private var lastBossSeen = 0L
    private var lastScreen = NetworkDefenseScreen.NONE
    private var startTaps = 0
    private var startVisibleSince = 0L
    private var finalBossArmed = false
    private var lastEvidenceAt = 0L

    fun isSessionActive(): Boolean = AutomationState.autoNetworkDefenseEnabled && sessionActive

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.autoNetworkDefenseEnabled) {
            if (sessionActive || pending || lastScreen != NetworkDefenseScreen.NONE) reset()
            return false
        }
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        fun pixel(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val detection = NetworkDefenseScreenDetector.detect(width, height, ::pixel)
        val now = SystemClock.elapsedRealtime()
        // Some emulator/accessibility combinations occasionally never deliver a gesture callback.
        if (pending && now - pendingSince >= PENDING_GESTURE_TIMEOUT) {
            pending = false
            pendingSince = 0L
            Log.w("DigiWorldNetwork", "Tap callback timeout; allowing a confirmed-screen retry")
        }
        if (detection.screen != NetworkDefenseScreen.NONE) AutoMoveController.pauseForPurchaseScreen()

        if (detection.screen == NetworkDefenseScreen.START) {
            if (!sessionActive || lastScreen != NetworkDefenseScreen.START) {
                sessionStarted = now
                startTaps = 0
                startVisibleSince = now
            }
            sessionActive = true
            lastEvidenceAt = now
            // A newly confirmed start dialog begins a fresh run. Never carry the previous boss arm.
            finalBossArmed = false
            lastBossSeen = 0L
            lastScreen = NetworkDefenseScreen.START
            showStatus(R.string.overlay_network_start)
            // Retry only while the complete START dialog remains positively detected. The next
            // state is confirmed by this dialog disappearing, not merely by dispatching a tap.
            if (AutomationState.enabled && now - startVisibleSince >= START_SETTLE_TIME) {
                tryTap(detection, now, START_RETRY_INTERVAL) { startTaps++ }
            }
            return true
        }

        startVisibleSince = 0L

        if (detection.screen == NetworkDefenseScreen.FINAL_BOSS) {
            // Accept a boss only after this loop started its own attempt.
            if (!sessionActive) return false
            sessionActive = true
            lastEvidenceAt = now
            finalBossArmed = true
            lastBossSeen = now
            lastScreen = NetworkDefenseScreen.FINAL_BOSS
            showStatus(R.string.overlay_network_give_up)
            if (AutomationState.enabled) tryTap(detection, now, BOSS_RETRY_INTERVAL)
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

        // Only a positively recognized bright Network Defense battle may display a dungeon status
        // or receive a give-up tap. Other game modes can have similar red/blue UI elements.
        if (detection.screen == NetworkDefenseScreen.BATTLE) {
            lastEvidenceAt = now
            lastScreen = NetworkDefenseScreen.BATTLE
            if (finalBossArmed) {
                lastBossSeen = now
                showStatus(R.string.overlay_network_give_up)
                if (AutomationState.enabled) tryTap(detection, now, BOSS_RETRY_INTERVAL)
            } else {
                showStatus(R.string.overlay_network_waiting)
            }
            return true
        }
        if (lastBossSeen != 0L && now - lastBossSeen < BOSS_STATUS_HOLD) {
            showStatus(R.string.overlay_network_give_up)
            return true
        }
        lastScreen = NetworkDefenseScreen.NONE
        DigiWorldAccessibilityService.instance?.showStatusOnly("", false)
        // Keep watching briefly through loading transitions, but never reserve unrelated frames.
        // This lets DigiWorld movement continue when the Network Defense toggle was left enabled.
        if (lastEvidenceAt != 0L && now - lastEvidenceAt >= SESSION_EVIDENCE_GRACE) {
            sessionActive = false
            finalBossArmed = false
            pending = false
            pendingSince = 0L
            sessionStarted = 0L
            lastEvidenceAt = 0L
            Log.i("DigiWorldNetwork", "Released stale Network Defense session")
        }
        return false
    }

    private fun tryTap(
        detection: NetworkDefenseDetection,
        now: Long,
        baseInterval: Long,
        afterSuccess: () -> Unit = {},
    ) {
        if (pending || now - lastTap < nextTapInterval) return
        val service = DigiWorldAccessibilityService.instance ?: return
        pending = true
        pendingSince = now
        lastTap = now
        nextTapInterval = SafeTapRandomizer.delay(baseInterval, (baseInterval / 8).coerceAtLeast(25L))
        service.dispatchNormalizedTap(detection.tapXRatio, detection.tapYRatio) { ok ->
            pending = false
            pendingSince = 0L
            if (ok) afterSuccess()
            Log.i("DigiWorldNetwork", "${detection.screen} tap=$ok normalized=${detection.tapXRatio},${detection.tapYRatio} confidence=${detection.confidence}")
        }
    }

    private fun showStatus(stringId: Int) {
        DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(stringId)) }
    }

    fun reset() {
        sessionActive = false
        pending = false
        pendingSince = 0L
        lastTap = 0L
        nextTapInterval = START_RETRY_INTERVAL
        sessionStarted = 0L
        lastBossSeen = 0L
        lastScreen = NetworkDefenseScreen.NONE
        startTaps = 0
        startVisibleSince = 0L
        finalBossArmed = false
        lastEvidenceAt = 0L
    }
}

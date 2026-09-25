package de.robinthor.digiworldexplorer.dungeon

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.input.SafeTapRandomizer
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object DungeonFrameAnalyzer {
    private const val TAP_INTERVAL = 1_200L
    private const val PENDING_TAP_TIMEOUT = 2_500L
    private const val REWARD_CLOSE_DELAY = 6_000L
    private const val ACTIVE_RUN_TIMEOUT = 120_000L
    private const val HASH_CHANGE_MIN = 5
    private var sessionActive = false
    private var pending = false
    private var pendingSince = 0L
    private var lastTap = 0L
    private var nextTapInterval = TAP_INTERVAL
    private var lastActivity = 0L
    private var lastHash = 0L
    private var lastScreen = DungeonScreen.NONE
    private var tapsOnScreen = 0
    private var stableDetections = 0
    private var rewardDetectedSince = 0L

    fun analyze(image: Image, width: Int, height: Int, resultsOnly: Boolean = false): Boolean {
        if (!resultsOnly && DungeonRotationRequest.phase == DungeonRotationRequest.Phase.PARKED) return true
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        val w = minOf(width, image.width)
        val h = minOf(height, image.height)
        if (w <= 0 || h <= 0 || (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= buffer.limit()) {
            return false
        }
        fun pixel(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val now = SystemClock.elapsedRealtime()
        // Android can drop a gesture callback while the game is being brought to the foreground.
        // Never let that interrupted first tap block the VS/Tower loop until the user restarts it.
        if (pending && now - pendingSince >= PENDING_TAP_TIMEOUT) {
            pending = false
            pendingSince = 0L
            Log.w("DigiWorldDungeon", "stale pending challenge tap released for retry")
        }
        val detection = DungeonScreenDetector.detect(w, h, ::pixel)
        if (resultsOnly && detection.screen != DungeonScreen.REWARD) return false
        val hash = frameHash(w, h, ::pixel)
        if (lastHash == 0L || java.lang.Long.bitCount(lastHash xor hash) >= HASH_CHANGE_MIN) lastActivity = now
        lastHash = hash

        if (detection.screen != DungeonScreen.NONE) {
            if (detection.screen == DungeonScreen.REWARD) DungeonRotationAnalyzer.onReward()
            AutoMoveController.pauseForPurchaseScreen()
            if (!AutomationState.autoDungeonEnabled) {
                sessionActive = false
                lastScreen = DungeonScreen.NONE
                DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(R.string.overlay_dungeon_disabled), sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.DUNGEON) }
                return true
            }
            sessionActive = true
            if (detection.screen != lastScreen) {
                lastScreen = detection.screen
                tapsOnScreen = 0
                stableDetections = 1
                lastActivity = now
                rewardDetectedSince = if (detection.screen == DungeonScreen.REWARD) now else 0L
            } else {
                stableDetections = (stableDetections + 1).coerceAtMost(3)
            }
            val service = DigiWorldAccessibilityService.instance
            service?.showStatusOnly(service.getString(if (AutomationState.autoDungeonEnabled) R.string.overlay_auto_dungeon else R.string.overlay_dungeon_disabled), sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.DUNGEON)
            // Slow devices may expose the finished-looking menu before its button accepts input.
            // Require a stable detection, then retry at a human-paced interval until the screen
            // actually changes instead of spending a fixed two-tap budget during loading.
            // Tower can reveal many Crest rewards one after another. The first reward frame is
            // already visually recognisable, but tapping it closes the screen before the reveal
            // sequence has finished. Wait, then close at the actual "Tap to close" area.
            val rewardReady = detection.screen != DungeonScreen.REWARD || now - rewardDetectedSince >= REWARD_CLOSE_DELAY
            if (!pending && stableDetections >= 2 && rewardReady && AutomationState.enabled && AutomationState.autoDungeonEnabled && now - lastTap >= nextTapInterval) {
                pending = true
                pendingSince = now
                lastTap = now
                nextTapInterval = SafeTapRandomizer.delay(TAP_INTERVAL, 35L)
                tapsOnScreen++
                // Tap the language-independent close prompt below the reward panel. Keeping this
                // at 82.5% works for compact 582x1280 phones as well as taller captures; if the
                // first gesture is swallowed, the still-visible reward screen triggers a retry.
                val tapY = if (detection.screen == DungeonScreen.REWARD) height * .825f else detection.tapY
                service?.dispatchSafeRandomizedTap(detection.tapX, tapY) { ok ->
                    pending = false
                    pendingSince = 0L
                    Log.i("DigiWorldDungeon", "${detection.screen} tap=$ok confidence=${detection.confidence}")
                }
                if (service == null) {
                    pending = false
                    pendingSince = 0L
                }
            }
            return true
        }

        stableDetections = 0
        if (!AutomationState.autoDungeonEnabled) sessionActive = false
        // A Tower battle and its loading transitions can take considerably longer than VS.
        // Keep the short guard for a stuck challenge menu, but do not disable the loop while
        // the run is active merely because no known menu is visible for 15 seconds.
        if (sessionActive && now - lastActivity >= ACTIVE_RUN_TIMEOUT) {
            stopForTimeout("active run", ACTIVE_RUN_TIMEOUT)
        }
        // Remember the VS/Tower run internally, but do not reserve unrelated frames while no
        // VS/Tower screen is visible. Otherwise an old dungeon session starves Feed, Summon and
        // DigiWorld detection until its timeout expires.
        return false
    }

    private fun stopForTimeout(context: String, timeout: Long) {
        sessionActive = false
        DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(R.string.overlay_dungeon_timeout), sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.DUNGEON) }
        Log.w("DigiWorldDungeon", "auto dungeon session released in $context after ${timeout / 1_000} seconds without visual progress; feature switch remains enabled")
    }

    private fun frameHash(width: Int, height: Int, argbAt: (Int, Int) -> Int): Long {
        val values = IntArray(64)
        var sum = 0
        var i = 0
        for (row in 0 until 8) for (col in 0 until 8) {
            val p = argbAt((width * (col + .5) / 8).toInt().coerceIn(0, width - 1), (height * (row + .5) / 8).toInt().coerceIn(0, height - 1))
            val gray = ((p shr 16 and 255) * 3 + (p shr 8 and 255) * 6 + (p and 255)) / 10
            values[i++] = gray
            sum += gray
        }
        val average = sum / values.size
        var hash = 0L
        values.forEachIndexed { index, value -> if (value >= average) hash = hash or (1L shl index) }
        return hash
    }

    fun isSessionActive() = sessionActive

    /**
     * The global failure-dialog detector owns the frame while it dismisses the dialog. The
     * challenge menu shown afterwards may be visually identical to the menu that started the
     * previous run, so clear the per-screen tap budget and treat it as a fresh screen.
     */
    fun onFailureDialogHandled() {
        DungeonRotationAnalyzer.onLoss()
        sessionActive = AutomationState.autoDungeonEnabled
        pending = false
        pendingSince = 0L
        lastScreen = DungeonScreen.NONE
        rewardDetectedSince = 0L
        tapsOnScreen = 0
        stableDetections = 0
        lastActivity = SystemClock.elapsedRealtime()
        lastHash = 0L
        Log.i("DigiWorldDungeon", "failure dialog handled - challenge retry state reset")
    }

    fun reset() { sessionActive = false; pending = false; pendingSince = 0L; lastTap = 0L; nextTapInterval = TAP_INTERVAL; lastActivity = 0L; lastHash = 0L; lastScreen = DungeonScreen.NONE; tapsOnScreen = 0; stableDetections = 0; rewardDetectedSince = 0L }
}

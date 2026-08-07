package de.robinthor.digiworldexplorer.dungeon

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

object DungeonFrameAnalyzer {
    private const val TAP_INTERVAL = 350L
    private const val INACTIVITY_TIMEOUT = 15_000L
    private const val HASH_CHANGE_MIN = 5
    private var sessionActive = false
    private var pending = false
    private var lastTap = 0L
    private var lastActivity = 0L
    private var lastHash = 0L
    private var lastScreen = DungeonScreen.NONE
    private var tapsOnScreen = 0

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        fun pixel(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val detection = DungeonScreenDetector.detect(width, height, ::pixel)
        val now = SystemClock.elapsedRealtime()
        val hash = frameHash(width, height, ::pixel)
        if (lastHash == 0L || java.lang.Long.bitCount(lastHash xor hash) >= HASH_CHANGE_MIN) lastActivity = now
        lastHash = hash

        if (detection.screen != DungeonScreen.NONE) {
            AutoMoveController.pauseForPurchaseScreen()
            if (!AutomationState.autoDungeonEnabled) {
                sessionActive = false
                lastScreen = DungeonScreen.NONE
                DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(R.string.overlay_dungeon_disabled)) }
                return true
            }
            sessionActive = true
            if (detection.screen != lastScreen) { lastScreen = detection.screen; tapsOnScreen = 0; lastActivity = now }
            if (now - lastActivity >= INACTIVITY_TIMEOUT) {
                stopForTimeout()
                return true
            }
            val service = DigiWorldAccessibilityService.instance
            service?.showStatusOnly(service.getString(if (AutomationState.autoDungeonEnabled) R.string.overlay_auto_dungeon else R.string.overlay_dungeon_disabled))
            if (!pending && AutomationState.enabled && AutomationState.autoDungeonEnabled && tapsOnScreen < 2 && now - lastTap >= TAP_INTERVAL) {
                pending = true
                lastTap = now
                tapsOnScreen++
                service?.dispatchValidatedTap(detection.tapX, detection.tapY) { ok ->
                    pending = false
                    Log.i("DigiWorldDungeon", "${detection.screen} tap=$ok confidence=${detection.confidence}")
                }
            }
            return true
        }

        if (!AutomationState.autoDungeonEnabled) sessionActive = false
        if (sessionActive && now - lastActivity >= INACTIVITY_TIMEOUT) {
            stopForTimeout()
        }
        return sessionActive
    }

    private fun stopForTimeout() {
        AutomationState.autoDungeonEnabled = false
        sessionActive = false
        DigiWorldAccessibilityService.instance?.let { it.showStatusOnly(it.getString(R.string.overlay_dungeon_timeout)) }
        Log.w("DigiWorldDungeon", "auto dungeon stopped after 15 seconds without visual progress")
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

    fun reset() { sessionActive = false; pending = false; lastTap = 0L; lastActivity = 0L; lastHash = 0L; lastScreen = DungeonScreen.NONE; tapsOnScreen = 0 }
}
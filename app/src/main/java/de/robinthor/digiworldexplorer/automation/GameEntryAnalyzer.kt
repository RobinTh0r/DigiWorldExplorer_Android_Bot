package de.robinthor.digiworldexplorer.automation

import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame

/** Live adapter for the tested entry controller. It owns only positively recognized entry screens. */
object GameEntryAnalyzer {
    @Volatile var observedScreen = ObservedScreen.LOGIN
        private set
    private var controller = GameEntryController()
    private var flowActive = false
    private var signature = EntryScreen.UNKNOWN
    private var confirmations = 0
    private var lastScan = 0L
    private var adClaimsThisFlow = 0

    @Synchronized
    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.enabled) { reset(); return false }
        val now = SystemClock.elapsedRealtime()
        if (now - lastScan < 300) return flowActive
        lastScan = now
        val plane = image.planes.firstOrNull() ?: return flowActive
        if (plane.pixelStride < 3) return flowActive
        val w = minOf(width, image.width)
        val h = minOf(height, image.height)
        val buffer = plane.buffer
        if (w <= 0 || h <= 0 || (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= buffer.limit()) return flowActive
        val frame = PixelFrame(w, h) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            (255 shl 24) or ((buffer.get(offset).toInt() and 255) shl 16) or
                ((buffer.get(offset + 1).toInt() and 255) shl 8) or (buffer.get(offset + 2).toInt() and 255)
        }
        val reading = GameEntryDetector.detect(frame)
        if (reading.screen == signature) confirmations++ else if (
            // Once the real start field was seen, a single animated/loading frame must not erase
            // that evidence. Two independently observed READY frames are still required to tap.
            reading.screen == EntryScreen.LOGIN_LOADING && signature == EntryScreen.LOGIN_READY
        ) {
            // retain READY evidence
        } else {
            signature = reading.screen
            confirmations = 1
        }
        val entryScreen = reading.screen in setOf(
            EntryScreen.LOGIN_LOADING, EntryScreen.LOGIN_READY, EntryScreen.IDLE_CLAIM,
            EntryScreen.IDLE_EMPTY, EntryScreen.RESULT, EntryScreen.NOTICE,
        )
        if (entryScreen) flowActive = true
        if (!flowActive) return false
        observedScreen = when (reading.screen) {
            EntryScreen.IDLE_CLAIM, EntryScreen.IDLE_EMPTY, EntryScreen.RESULT -> ObservedScreen.IDLE_REWARDS
            EntryScreen.HOME -> ObservedScreen.HOME
            else -> ObservedScreen.LOGIN
        }
        if (reading.screen == EntryScreen.HOME) {
            controller.tick(EntryScreen.HOME, now)
            flowActive = false
            ScreenDirector.noteAction("Entry complete: Home", ObservedScreen.HOME)
            return false
        }
        val service = DigiWorldAccessibilityService.instance ?: return true
        val rawRemaining = reading.adRemaining
        if (rawRemaining == 0) EntryDailyStore.markAdsDone(service)
        val effectiveRemaining = when {
            EntryDailyStore.adsDone(service) -> 0
            rawRemaining == null -> null
            else -> (rawRemaining - adClaimsThisFlow).coerceAtLeast(0)
        }
        val confirmed = reading.screen != EntryScreen.LOGIN_READY || (signature == EntryScreen.LOGIN_READY && confirmations >= 2)
        val action = if (confirmed) controller.tick(
            reading.screen, now, AutomationState.adSkipPassEnabled && !EntryDailyStore.adsDone(service), effectiveRemaining,
        ) else EntryAction.WAIT
        val label = when (action) {
            EntryAction.WAIT -> when (reading.screen) {
                EntryScreen.LOGIN_LOADING -> "Waiting for Touch to Start"
                EntryScreen.LOGIN_READY -> "Touch to Start confirmed"
                else -> "Checking entry rewards"
            }
            EntryAction.TOUCH_START -> "Touch to Start"
            EntryAction.CLAIM_IDLE -> "Claiming idle rewards"
            EntryAction.CLAIM_AD -> "Claiming idle Ad Skip reward"
            EntryAction.CLOSE_RESULT -> "Closing reward result"
            EntryAction.CLOSE_IDLE -> "Closing idle rewards"
            EntryAction.CLOSE_NOTICE -> "Closing update notice"
            EntryAction.PARK -> "Entry paused: screen not confirmed"
        }
        ScreenDirector.noteAction(label, observedScreen)
        val target = when (action) {
            EntryAction.TOUCH_START -> NormalizedPoint(.50, .843)
            EntryAction.CLAIM_IDLE -> NormalizedPoint(.632, .74)
            EntryAction.CLAIM_AD -> NormalizedPoint(.37, .74)
            EntryAction.CLOSE_RESULT -> NormalizedPoint(.90, .82)
            EntryAction.CLOSE_IDLE -> NormalizedPoint(.92, .55)
            EntryAction.CLOSE_NOTICE -> NormalizedPoint(.94, .52)
            else -> null
        }
        if (action == EntryAction.CLAIM_AD) {
            adClaimsThisFlow++
            if (adClaimsThisFlow >= 2) EntryDailyStore.markAdsDone(service)
        }
        if (target != null) {
            // Entry fixtures and the title screen cover the complete captured game surface.
            // Viewport remapping shifts the title target upward on BlueStacks.
            val x = (target.x * w).toFloat()
            val y = (target.y * h).toFloat()
            AutomationEventLog.record(AutomationEventKind.ACTION_DISPATCHED, "ENTRY_${action.name}")
            service.dispatchValidatedTap(x, y) { success ->
                if (!success) synchronized(this) { controller.cancel() }
            }
        }
        return true
    }

    @Synchronized fun reset() {
        controller = GameEntryController()
        flowActive = false
        signature = EntryScreen.UNKNOWN
        confirmations = 0
        lastScan = 0L
        adClaimsThisFlow = 0
        observedScreen = ObservedScreen.LOGIN
    }
}

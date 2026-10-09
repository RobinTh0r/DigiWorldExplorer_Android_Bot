package de.robinthor.digiworldexplorer.feed

import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.automation.*
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.vision.*

object BondRotationAnalyzer {
    private var rotation = BondRotation()
    private var scanAt = 0L
    private var signature = ""
    private var matches = 0
    private var owns = false
    private var lastObservation = ""
    private var popupCloseAt = 0L
    private var popupCloseAttempts = 0
    private var popupCollectStartedAt = 0L
    var observedScreen = ObservedScreen.UNKNOWN
        private set
    @Volatile private var fastPolling = false
    fun reset() { rotation = BondRotation(); scanAt = 0; signature = ""; matches = 0; owns = false; fastPolling = false; lastObservation = ""; observedScreen = ObservedScreen.UNKNOWN; popupCloseAt = 0; popupCloseAttempts = 0; popupCollectStartedAt = 0 }
    fun needsFastPolling() = fastPolling
    fun ownsSession() = AutomationState.enabled &&
        (BondRotationRequest.active() || (DigiCopilotRequest.active() && AutomationState.autoBondRotationEnabled)) &&
        (rotation.ownsFrame() || rotation.step == BondStep.COLLECT)

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val forced = BondRotationRequest.active()
        val scheduled = DigiCopilotRequest.active() && AutomationState.autoBondRotationEnabled
        if (!AutomationState.enabled || (!forced && !scheduled)) {
            reset(); return false
        }
        val now = SystemClock.elapsedRealtime()
        fastPolling = rotation.fastBubblePolling(now)
        if (now < scanAt) return owns
        scanAt = now + if (fastPolling) 100 else 500
        // The owning tour delegates this Home recovery itself; exclusive ownership must not
        // suppress the existing stage-failure dismissal while waiting for the Bond bubble.
        if (rotation.step in setOf(BondStep.HOME, BondStep.COLLECT) &&
            StageFailedFrameAnalyzer.analyze(image, width, height)) return true
        val plane = image.planes.firstOrNull() ?: return owns
        val w = minOf(width, image.width); val h = minOf(height, image.height)
        val bytes = plane.buffer
        if (plane.pixelStride < 3 || w <= 0 || h <= 0 ||
            (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= bytes.limit()) return owns
        val frame = PixelFrame(w, h) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            (255 shl 24) or ((bytes.get(offset).toInt() and 255) shl 16) or
                ((bytes.get(offset + 1).toInt() and 255) shl 8) or (bytes.get(offset + 2).toInt() and 255)
        }
        // Use the same animation-tolerant Home decision as the Director/entry flow.
        val grid = PartnerGridDetector.detect(frame)
        val home = GameEntryDetector.detect(frame, partnerReading = grid).screen == EntryScreen.HOME
        if (rotation.step == BondStep.COLLECT && popupCollectStartedAt != rotation.collectStartedAt) {
            popupCollectStartedAt = rotation.collectStartedAt
            popupCloseAttempts = 0
        }
        if (rotation.step == BondStep.COLLECT && !home && PartnerDetailPopupDetector.detect(frame)) {
            observedScreen = ObservedScreen.MESSAGE
            if (now - popupCloseAt >= 1_500L && popupCloseAttempts < 2) {
                popupCloseAt = now
                popupCloseAttempts++
                de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                    "BOND.POPUP", "Closing recognized Partner detail card attempt=$popupCloseAttempts")
                de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.requestScreenshot("bond-partner-detail-popup")
                DigiWorldAccessibilityService.instance?.dispatchBack()
            }
            return true
        }
        observedScreen = when {
            grid.confirmation -> ObservedScreen.MESSAGE
            grid.page -> ObservedScreen.PARTNER_PAGE
            home -> ObservedScreen.HOME
            else -> ObservedScreen.UNKNOWN
        }
        val key = "$home|${grid.page}|${grid.expanded}|${grid.raised}|${grid.selected}|${grid.canRaise}|${grid.confirmation}|${grid.digimonSection}"
        val observation = "step=${rotation.step} home=$home page=${grid.page} section=${grid.digimonSection} " +
            "expanded=${grid.expanded} cells=${grid.cells.size} raised=${grid.raised} selected=${grid.selected} " +
            "canRaise=${grid.canRaise} confirmation=${grid.confirmation}"
        if (observation != lastObservation) {
            lastObservation = observation
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("BOND.OBSERVE", observation)
        }
        if (key == signature) matches++ else { signature = key; matches = 1 }
        // A short-lived bubble must not wait for three identical scene classifications.
        if (matches < 3 && rotation.step != BondStep.COLLECT) return owns
        val previous = rotation.step
        val bubble = home && BondBubbleDetector.detect(frame) != null
        // Evidence belongs to the current COLLECT only. On HOME entry the timestamp still
        // belongs to the previous partner until tick() starts the new collection window.
        val collectedSettled = previous == BondStep.COLLECT &&
            FeedFrameAnalyzer.collectionSettledSince(rotation.collectStartedAt, now, settleMillis = 2_500L)
        val command = rotation.tick(home, grid, FeedFrameAnalyzer.isBusy(), now, bubble,
            forced || BondCycleTimer.canStartBond(now),
            collectedSettled)
        fastPolling = rotation.fastBubblePolling(now)
        if (matches == 3) android.util.Log.i(
            "DigiWorldBond",
            "probe step=${rotation.step} home=$home forced=$forced scheduled=$scheduled " +
                "feedBusy=${FeedFrameAnalyzer.isBusy()} gridPage=${grid.page} bubble=$bubble command=${command?.step}",
        )
        // COLLECT delegates the actual detector to FeedFrameAnalyzer below, but it must still
        // exclusively own the frame. Otherwise the later passive feed probe can reset the
        // shared collection state when Auto Feed itself is disabled, causing a second bubble
        // tap and leaving the rotation parked instead of opening the next partner.
        owns = rotation.ownsFrame() || rotation.step == BondStep.COLLECT
        if (previous != BondStep.COLLECT && rotation.step == BondStep.COLLECT) {
            FeedFrameAnalyzer.allowImmediateScan()
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                "BOND.COLLECT", "waiting for bubble visited=${rotation.visited}")
            DigiWorldAccessibilityService.instance?.let { service ->
                service.showStatusOnly(service.getString(de.robinthor.digiworldexplorer.R.string.bond_wait_bubble, rotation.visited))
            }
        }
        val bubbleNotFound = previous == BondStep.COLLECT && rotation.step != BondStep.COLLECT &&
            !collectedSettled && !rotation.bubbleSeenDuringCollect && rotation.step != BondStep.PARK
        if (previous == BondStep.COLLECT && rotation.step != BondStep.COLLECT) {
            val outcome = when {
                rotation.step == BondStep.PARK -> rotation.pauseReason.name.lowercase()
                collectedSettled -> "bubble-collected"
                bubbleNotFound -> "bubble-not-found"
                else -> "bubble-seen-unconfirmed"
            }
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                "BOND.COLLECT", "outcome=$outcome visited=${rotation.visited} home=$home")
        }
        if (rotation.step == BondStep.COLLECT) {
            // This Home boundary was confirmed by the rotation itself. Delegating here avoids the
            // stricter passive Home fingerprint rejecting battle-animation frames with a bubble.
            // Once a tap has been accepted, freeze collection until its one-second settle period
            // completes instead of tapping the still-visible animated bubble again.
            if (FeedFrameAnalyzer.collectedSince(rotation.collectStartedAt)) return true
            if (!home) return true
            return FeedFrameAnalyzer.analyze(image, w, h, homeAlreadyConfirmed = true, rotationOwned = true)
        }
        if (previous != BondStep.REST && rotation.step == BondStep.REST) {
            BondRotationRequest.complete()
            BondCycleTimer.bondCompleted(now)
            if (AutomationState.autoFarmEnabled) BondFarmAnalyzer.requestVisit()
            else if (AutomationState.copilotRewardsEnabled) {
                BondCycleTimer.farmReturnedHome(now)
                HomeIdleRewardRequest.start()
            } else if (AutomationState.copilotDwsEnabled) {
                BondCycleTimer.farmReturnedHome(now)
                DwsExcursionRequest.start()
            } else {
                BondCycleTimer.farmReturnedHome(now)
                DigiCopilotRequest.stop("Selected modules complete")
            }
            android.util.Log.i("DigiWorldBond", "complete visited=${rotation.visited} restored=${rotation.original}; waiting for next bubble after farm")
        }
        val service = DigiWorldAccessibilityService.instance ?: return owns
        if (rotation.step == BondStep.PARK) {
            if (previous != BondStep.PARK) {
                val (reason, status) = when (rotation.pauseReason) {
                    BondPauseReason.PARTNER_NOT_CONFIRMED -> "Partner screen not confirmed" to service.getString(de.robinthor.digiworldexplorer.R.string.bond_partner_unconfirmed)
                    BondPauseReason.HOME_NOT_CONFIRMED -> "Home screen not confirmed" to service.getString(de.robinthor.digiworldexplorer.R.string.bond_home_unconfirmed)
                    BondPauseReason.BUBBLE_NOT_FOUND -> "Bond bubble not found" to service.getString(de.robinthor.digiworldexplorer.R.string.bond_bubble_not_found)
                    BondPauseReason.BUBBLE_COLLECTION_UNCONFIRMED ->
                        "Bond bubble collection unconfirmed" to service.getString(de.robinthor.digiworldexplorer.R.string.bond_bubble_unconfirmed)
                }
                BondRotationRequest.park(reason)
                de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
                    "BOND.PARK", "reason=${rotation.pauseReason} $observation")
                de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.requestScreenshot(
                    "bond-park-${rotation.pauseReason.name.lowercase()}")
                service.showStatusOnly(status)
            }
            return true
        }
        if (command == null) return owns
        val target = when (command.step) {
            BondStep.OPEN -> if (tapViewportIsTall(w, h)) NormalizedPoint(.205, .956) else NormalizedPoint(.254, .956)
            BondStep.PARTNER_TAB -> grid.partnerTabTarget
            BondStep.EXPAND -> grid.expandTarget
            BondStep.SELECT -> grid.cells.getOrNull(command.cell ?: -1)?.let { point ->
                // The expand/collapse control overlaps the lower-right portrait on tall phones.
                // Tap safely inside that portrait's upper-left quadrant instead of its centre.
                if (command.cell == 14 && tapViewportIsTall(w, h))
                    NormalizedPoint(point.x - .035, point.y - .025) else point
            }
            BondStep.RAISE -> grid.raiseTarget
            BondStep.CONFIRM -> grid.confirmationTarget ?: return true
            BondStep.HOME -> NormalizedPoint(.5, .947)
            else -> null
        } ?: return owns
        FeedFrameAnalyzer.pauseForDigiWorld()
        val tapViewport = if (command.step == BondStep.OPEN) {
            HomeScreenDetector.viewport(w, h, frame::argbAt)
                ?: if (previous == BondStep.COLLECT && collectedSettled) GameViewport.fit(w, h) else return owns
        } else GameViewport.fit(w,h)
        val (x,y) = tapViewport.pixel(target)
        service.showStatusOnly(if (bubbleNotFound) service.getString(de.robinthor.digiworldexplorer.R.string.bond_bubble_not_found)
            else "Bond ${rotation.visited}/15: ${command.step.name.lowercase()}")
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record(
            "BOND.ACTION",
            "step=${command.step} cell=${command.cell} original=${rotation.original} visited=${rotation.visited} " +
                "page=${grid.page} expanded=${grid.expanded} raised=${grid.raised} selected=${grid.selected} " +
                "canRaise=${grid.canRaise} confirmation=${grid.confirmation} target=$target",
        )
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.requestScreenshot("bond-${command.step.name.lowercase()}")
        android.util.Log.i("DigiWorldBond", "step=${command.step} cell=${command.cell} original=${rotation.original} visited=${rotation.visited}")
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { success ->
            // Gesture callbacks can be cancelled by the game's immediate window transition even
            // when the tap was accepted. Only subsequent visual state or the bounded deadline may
            // confirm/fail the step; a callback alone must not destroy the whole rotation.
            if (!success) android.util.Log.w("DigiWorldBond", "gesture callback cancelled for ${command.step}; awaiting visual proof")
        }
        matches = 0
        return true
    }

    private fun tapViewportIsTall(width: Int, height: Int) = GameViewport.fit(width, height).usesTallPhoneLayout
}

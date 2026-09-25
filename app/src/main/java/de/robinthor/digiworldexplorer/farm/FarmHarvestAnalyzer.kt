package de.robinthor.digiworldexplorer.farm

import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.automation.AutomationEventKind
import de.robinthor.digiworldexplorer.automation.AutomationEventLog
import de.robinthor.digiworldexplorer.automation.ScreenDirector
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame

/**
 * Opt-in, manually opened Meat Field workflow. Every screen must agree twice before the controller
 * may act, and every tap is followed by visual proof. Navigation to/from the field remains disabled.
 */
object FarmHarvestAnalyzer {
    @Volatile var visitComplete = false
        private set
    private var scanAt = 0L
    private var ownsFrame = false
    private var flowActive = false
    private var signature: String? = null
    private var confirmations = 0
    private var controller = FarmController()
    private var lastDispatched: FarmCommand? = null
    private var parked = false
    private var parkedUntil = 0L
    private var epoch = 0L

    @Synchronized
    fun analyze(image: Image, width: Int, height: Int): Boolean {
        if (!AutomationState.autoFarmEnabled) { reset(); return false }
        val now = SystemClock.elapsedRealtime()
        if (parked && now >= parkedUntil) {
            // A timeout ends only the unverified transaction, not the whole opt-in farm feature.
            // Rebuild the controller and require two fresh matching frames before another tap.
            controller = FarmController()
            lastDispatched = null
            signature = null
            confirmations = 0
            parked = false
        }
        if (now < scanAt) return ownsFrame
        scanAt = now + 450
        val plane = image.planes.firstOrNull() ?: return ownsFrame
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        val w = minOf(width, image.width)
        val h = minOf(height, image.height)
        if (w <= 0 || h <= 0 || (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= buffer.limit()) return false
        val pixels = PixelFrame(w, h) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            (255 shl 24) or ((buffer.get(offset).toInt() and 255) shl 16) or
                ((buffer.get(offset + 1).toInt() and 255) shl 8) or (buffer.get(offset + 2).toInt() and 255)
        }
        val viewport = GameViewport.fit(w, h)
        val field = FarmHarvestDetector.detect(pixels, viewport)
        val dialog = if (!field.field)
            FarmDialogDetector.detect(pixels, field.visiblePlots, viewport, trustedFarmFlow = flowActive) else FarmDialogDetection()
        val observation = when {
            field.field -> {
                val seedCounts = FarmResourceReaders.hudSeeds(pixels, viewport)
                FarmObservation(
                    view = FarmView.FIELD,
                    plots = field.states,
                    freeSeeds = seedCounts.filterNotNull().sum().takeIf { seedCounts.any { count -> count != null } },
                    seedCounts = seedCounts,
                    blockedPlots = field.bubbleBlocked,
                    wateringCans = FarmResourceReaders.wateringCans(pixels, viewport),
                    wateringPriorities = field.wateringPriorities,
                    wateringEnabled = AutomationState.farmWateringEnabled,
                    adSkipPass = AutomationState.adSkipPassEnabled,
                )
            }
            dialog.view == FarmView.SEEDS -> FarmObservation(
                view = FarmView.SEEDS,
                // Unknown is not inventory. Only a positively read value may spend a seed.
                freeSlot = dialog.seedCounts.indices.reversed().firstOrNull { (dialog.seedCounts[it] ?: 0) > 0 },
                seedCounts = dialog.seedCounts,
                selectedSlot = dialog.selectedSlot,
            )
            dialog.view == FarmView.WATER -> FarmObservation(FarmView.WATER)
            dialog.view == FarmView.ERROR -> FarmObservation(FarmView.ERROR)
            else -> FarmObservation(FarmView.UNKNOWN, stable = false)
        }
        // A dialog is only trusted after this analyzer has positively recognized the field and
        // dispatched an action. Standalone dialog-like colours on unrelated screens must not claim
        // ownership or start a farm flow.
        val recognized = field.field || (flowActive && observation.view != FarmView.UNKNOWN)
        ownsFrame = recognized || flowActive
        if (!AutomationState.enabled) { reset(); return recognized }
        if (recognized) AutoMoveController.pauseForPurchaseScreen()
        if (!ownsFrame) return false
        val service = DigiWorldAccessibilityService.instance ?: return ownsFrame
        service.showStatusOnly(service.getString(if (parked) R.string.farm_parked else R.string.farm_harvest_status), sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.MEAT_FIELD)
        if (parked) return ownsFrame

        val nextSignature = if (recognized) observation.signature() else null
        if (nextSignature != null && nextSignature == signature) confirmations++ else {
            signature = nextSignature
            confirmations = if (nextSignature == null) 0 else 1
        }
        // Harvest animations can briefly erase a neighbouring yield bubble, and the green
        // dialog button appears before it accepts input. After an action (and throughout a
        // dialog) demand roughly 1.8 s of identical evidence instead of the normal two frames.
        val requiredConfirmations = if (lastDispatched != null || observation.view == FarmView.SEEDS) 4 else 2
        val stableObservation = observation.copy(stable = recognized && confirmations >= requiredConfirmations)
        val command = controller.tick(stableObservation, now)
        visitComplete = command.operation == FarmOperation.COMPLETE && stableObservation.stable
        when (command.operation) {
            FarmOperation.WAIT -> return ownsFrame
            FarmOperation.COMPLETE -> {
                verifyPriorAction()
                flowActive = false
                service.showStatusOnly("Farm: nothing ready", sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.MEAT_FIELD)
                return recognized
            }
            FarmOperation.PARK -> {
                parked = true
                parkedUntil = now + 2_500
                flowActive = false
                val plotCodes = observation.plots.joinToString("") { it.name.take(1) }
                service.showStatusOnly("Farm paused: ${plotCodes.ifEmpty { "unrecognized" }}", sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.MEAT_FIELD)
                android.util.Log.w("DigiWorldFarm", "park view=${observation.view} plots=${observation.plots} seeds=${observation.freeSeeds}")
                AutomationEventLog.record(AutomationEventKind.PARKED, "FARM_UNVERIFIED")
                return recognized
            }
            else -> Unit
        }
        if (command.operation == FarmOperation.CLOSE_WATER) lastDispatched = null else verifyPriorAction()
        val target = command.target(dialog) ?: run {
            controller.cancel()
            parked = true
            AutomationEventLog.record(AutomationEventKind.PARKED, "FARM_TARGET_MISSING")
            return recognized
        }
        val (x, y) = viewport.pixel(target)
        flowActive = true
        lastDispatched = command
        signature = null
        confirmations = 0
        val action = command.eventName()
        android.util.Log.i(
            "DigiWorldFarm",
            "dispatch=$action view=${observation.view} plots=${observation.plots} seeds=${observation.freeSeeds} " +
                "freeSlot=${observation.freeSlot} selected=${observation.selectedSlot} " +
                "seedCounts=${observation.seedCounts} water=${observation.wateringPriorities} " +
                "cans=${observation.wateringCans} target=$target",
        )
        ScreenDirector.noteAction(action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, de.robinthor.digiworldexplorer.automation.ObservedScreen.MEAT_FIELD)
        val dispatchEpoch = epoch
        AutomationEventLog.record(AutomationEventKind.ACTION_DISPATCHED, action)
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { success ->
            synchronized(this) {
                if (epoch == dispatchEpoch && !success) {
                    controller.cancel()
                    parked = true
                    flowActive = false
                    AutomationEventLog.record(AutomationEventKind.PARKED, "FARM_GESTURE_REJECTED")
                }
            }
        }
        return true
    }

    private fun FarmObservation.signature() = listOf(
        view.name,
        plots.joinToString(",") { it.name },
        freeSeeds?.toString() ?: "?",
        freeSlot?.toString() ?: "?",
        selectedSlot?.toString() ?: "?",
        blockedPlots.sorted().joinToString(","),
        wateringCans?.toString() ?: "?",
        wateringPriorities.toSortedMap().entries.joinToString(",") { "${it.key}:${it.value}" },
    ).joinToString("|")

    private fun FarmCommand.target(dialog: FarmDialogDetection): NormalizedPoint? = when (operation) {
        FarmOperation.HARVEST, FarmOperation.OPEN_SEEDS -> plot?.let(FarmHarvestDetector::target)
        FarmOperation.OPEN_WATER -> plot?.let(FarmHarvestDetector::waterTarget)
        FarmOperation.SELECT_FREE_SEED -> slot?.let { dialog.slots.getOrNull(it) }
        FarmOperation.CONFIRM_SEED, FarmOperation.SELECT_WATER, FarmOperation.CONFIRM_WATER -> dialog.selectButton
        FarmOperation.CLOSE_WATER, FarmOperation.CLOSE_ERROR -> dialog.closeTarget
        else -> null
    }

    private fun FarmCommand.eventName() = "FARM_${operation.name}" + (plot?.let { "_$it" } ?: "")

    private fun verifyPriorAction() {
        lastDispatched?.let {
            AutomationEventLog.record(AutomationEventKind.ACTION_VERIFIED, it.eventName())
            lastDispatched = null
        }
    }

    @Synchronized
    fun reset() {
        visitComplete = false
        epoch++
        scanAt = 0
        ownsFrame = false
        flowActive = false
        signature = null
        confirmations = 0
        controller = FarmController()
        lastDispatched = null
        parked = false
        parkedUntil = 0L
    }
}

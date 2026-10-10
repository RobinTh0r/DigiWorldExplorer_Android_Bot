package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.capture.AnalysisImage as Image
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
    private var unresolvedFieldSince = 0L
    private var lastObservation=""

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
        // The translucent seed/water dialog leaves the six plots visible behind it on real
        // phones. Detect it on every active farm transaction and give that foreground evidence
        // precedence over the background field.
        val dialog = FarmDialogDetector.detect(pixels, field.visiblePlots, viewport, trustedFarmFlow = flowActive)
        val observation = when {
            flowActive && dialog.view == FarmView.SEEDS -> FarmObservation(
                view = FarmView.SEEDS,
                // Unknown is not inventory. Only a positively read value may spend a seed.
                freeSlot = dialog.seedCounts.indices.reversed().firstOrNull { (dialog.seedCounts[it] ?: 0) > 0 },
                seedCounts = dialog.seedCounts,
                selectedSlot = dialog.selectedSlot,
            )
            flowActive && dialog.view == FarmView.WATER -> FarmObservation(FarmView.WATER)
            flowActive && dialog.view == FarmView.ERROR -> FarmObservation(FarmView.ERROR)
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
            else -> FarmObservation(FarmView.UNKNOWN, stable = false)
        }
        // A dialog is only trusted after this analyzer has positively recognized the field and
        // dispatched an action. Standalone dialog-like colours on unrelated screens must not claim
        // ownership or start a farm flow.
        val recognized = field.field || (flowActive && observation.view != FarmView.UNKNOWN)
        val detail="view=${observation.view} plots=${observation.plots} seeds=${observation.seedCounts} " +
            "water=${observation.wateringPriorities} centers=${field.plotCenters}"
        if(detail!=lastObservation) {
            lastObservation=detail
            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("FARM.OBSERVE",detail)
        }
        ownsFrame = recognized || flowActive
        if (!AutomationState.enabled) { reset(); return recognized }
        if (recognized) AutoMoveController.pauseForPurchaseScreen()
        if (!ownsFrame) return false
        val service = DigiWorldAccessibilityService.instance ?: return ownsFrame
        service.showStatusOnly(service.getString(if (parked) R.string.farm_parked else R.string.farm_harvest_status), sourceScreen = de.robinthor.digiworldexplorer.automation.ObservedScreen.MEAT_FIELD)
        if (parked) return ownsFrame

        // UNKNOWN is not proof that the field is finished: on a real phone an empty-plot badge
        // can be hidden for a frame by animation. Keep scanning, and preserve one useful frame in
        // diagnostics so a new device layout can be measured instead of guessed at.
        val unresolvedField = field.field && observation.plots.any { it == PlotState.UNKNOWN }
        if (unresolvedField) {
            if (unresolvedFieldSince == 0L) unresolvedFieldSince = now
            if (now - unresolvedFieldSince >= 2_000L) {
                de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.requestScreenshot("farm-field-unresolved")
                unresolvedFieldSince = now + 28_000L
            }
        } else unresolvedFieldSince = 0L

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
        if (command.operation == FarmOperation.CLOSE_WATER) lastDispatched = null
        else if(command!=lastDispatched)verifyPriorAction()
        val target = command.target(dialog,field) ?: run {
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
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("FARM.ACTION",
            "operation=$action view=${observation.view} plots=${observation.plots} seedCounts=${observation.seedCounts} " +
                "slot=${observation.freeSlot} selected=${observation.selectedSlot} cans=${observation.wateringCans} adSkipPass=${observation.adSkipPass} target=$target")
        ScreenDirector.noteAction(action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, de.robinthor.digiworldexplorer.automation.ObservedScreen.MEAT_FIELD)
        val dispatchEpoch = epoch
        AutomationEventLog.record(AutomationEventKind.ACTION_DISPATCHED, action)
        service.dispatchValidatedTap(x.toFloat(), y.toFloat()) { success ->
            synchronized(this) {
                if (epoch == dispatchEpoch && !success) {
                    // Unity often replaces the window before Android reports gesture completion.
                    // Keep the bounded controller transaction alive; the following field/dialog
                    // frame is the proof, and its deadline still parks a genuinely missed tap.
                    android.util.Log.w("DigiWorldFarm", "gesture callback cancelled for $action; awaiting visual proof")
                }
            }
        }
        return true
    }

    private fun FarmObservation.signature(): String {
        // Exact counters are OCR hints, not screen identity. Animation can make 15 alternate with
        // 12 on adjacent frames. The controller only needs to know whether a free seed exists;
        // retaining exact values here used to prevent an otherwise proven empty field from ever
        // reaching OPEN_SEEDS.
        val seedAvailability = when {
            freeSeeds == null -> "?"
            freeSeeds == 0 -> "0"
            else -> "+"
        }
        val waterAvailability = when {
            wateringCans == null -> "?"
            wateringCans == 0 -> "0"
            else -> "+"
        }
        return listOf(
            view.name,
            plots.joinToString(",") { it.name },
            seedAvailability,
            freeSlot?.toString() ?: "?",
            selectedSlot?.toString() ?: "?",
            blockedPlots.sorted().joinToString(","),
            waterAvailability,
            wateringPriorities.keys.sorted().joinToString(","),
        ).joinToString("|")
    }

    private fun FarmCommand.target(dialog: FarmDialogDetection,field:FarmHarvestDetection): NormalizedPoint? = when (operation) {
        FarmOperation.HARVEST, FarmOperation.OPEN_SEEDS -> plot?.let { index ->
            field.actionTargets[index] ?: field.plotCenters.getOrNull(index)?.let {
                it.copy(x=it.x+if(index%2==0).07 else -.07)
            }
        }
        FarmOperation.OPEN_WATER -> plot?.let { field.waterTargets[it] }
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
        unresolvedFieldSince = 0L
        lastObservation=""
    }
}

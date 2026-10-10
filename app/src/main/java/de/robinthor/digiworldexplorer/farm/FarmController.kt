package de.robinthor.digiworldexplorer.farm

enum class PlotState { UNKNOWN, LOCKED, EMPTY, GROWING, RIPE }
enum class FarmView { UNKNOWN, FIELD, SEEDS, WATER, ERROR }
enum class FarmOperation { WAIT, HARVEST, OPEN_SEEDS, SELECT_FREE_SEED, CONFIRM_SEED, OPEN_WATER, SELECT_WATER, CONFIRM_WATER, CLOSE_WATER, CLOSE_ERROR, COMPLETE, PARK }
data class FarmCommand(val operation: FarmOperation, val plot: Int? = null, val slot: Int? = null)

/** Facts from a single stable frame. Missing counters/identities are unknown, never zero. */
data class FarmObservation(
    val view: FarmView,
    val plots: List<PlotState> = emptyList(),
    val freeSeeds: Int? = null,
    val freeSlot: Int? = null,
    val seedCounts: List<Int?> = emptyList(),
    val selectedSlot: Int? = null,
    val blockedPlots: Set<Int> = emptySet(),
    val wateringCans: Int? = null,
    val wateringPriorities: Map<Int, Int> = emptyMap(),
    val wateringEnabled: Boolean = false,
    val adSkipPass: Boolean = false,
    val stable: Boolean = true,
)

/**
 * Frame-driven harvest/plant workflow. Each non-WAIT result is a single command; the caller must
 * dispatch it once. Completion requires new visual evidence, not a gesture callback. Intentionally
 * The controller retains the last proven seed counter while the seed dialog covers it.
 */
class FarmController(private val timeoutMillis: Long = 30_000) {
    private var pending: FarmCommand? = null
    private var deadline = 0L
    private var issuedAt = 0L
    private var retries = 0
    private var seedCountBefore: Int? = null
    private var plantingPlot: Int? = null
    private var terminal: FarmOperation? = null
    var harvested = 0
        private set
    var planted = 0
        private set
    private val plantedPlots = mutableSetOf<Int>()

    init { require(timeoutMillis > 0) }

    fun tick(frame: FarmObservation, now: Long): FarmCommand {
        terminal?.let { return FarmCommand(it) }
        val waiting = pending
        val expired = waiting != null && now >= deadline
        if (!frame.stable) return if (expired) park() else FarmCommand(FarmOperation.WAIT)

        // Re-send only when the exact safe source screen is still positively visible. This
        // covers swallowed real-device taps without turning Farm into coordinate spam.
        if (waiting != null && now - issuedAt >= 1_500L && retries < 2) {
            val stillNeedsTap = when (waiting.operation) {
                FarmOperation.HARVEST -> frame.view == FarmView.FIELD &&
                    frame.plots.getOrNull(waiting.plot ?: -1) == PlotState.RIPE
                FarmOperation.OPEN_SEEDS -> frame.view == FarmView.FIELD &&
                    frame.plots.getOrNull(waiting.plot ?: -1) == PlotState.EMPTY
                FarmOperation.CONFIRM_SEED -> frame.view == FarmView.SEEDS
                FarmOperation.OPEN_WATER, FarmOperation.CONFIRM_WATER -> frame.view == FarmView.FIELD
                FarmOperation.SELECT_WATER -> frame.view == FarmView.WATER
                FarmOperation.CLOSE_WATER -> frame.view == FarmView.WATER
                FarmOperation.CLOSE_ERROR -> frame.view == FarmView.ERROR
                else -> false
            }
            if (stillNeedsTap) {
                issuedAt = now
                retries++
                return waiting
            }
        }

        // Water is never an alternative planting option. Close once, then verify the field.
        if (frame.view == FarmView.WATER && waiting?.operation !in setOf(FarmOperation.OPEN_WATER, FarmOperation.SELECT_WATER, FarmOperation.CONFIRM_WATER, FarmOperation.CLOSE_WATER)) {
            return issue(FarmCommand(FarmOperation.CLOSE_WATER), now)
        }
        if (frame.view == FarmView.ERROR && waiting?.operation != FarmOperation.CLOSE_ERROR) {
            return issue(FarmCommand(FarmOperation.CLOSE_ERROR), now)
        }
        if (waiting != null) {
            when (waiting.operation) {
                FarmOperation.CLOSE_WATER -> {
                    if (frame.view != FarmView.FIELD) return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    // The prior flow was interrupted: preserve counts and require a new manual run.
                    return park()
                }
                FarmOperation.CLOSE_ERROR -> {
                    if (frame.view != FarmView.FIELD) return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    pending = null
                    plantingPlot = null
                }
                FarmOperation.HARVEST -> {
                    if (frame.view != FarmView.FIELD || frame.plots.getOrNull(waiting.plot!!) != PlotState.EMPTY)
                        return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    harvested++
                    pending = null
                }
                FarmOperation.OPEN_SEEDS -> {
                    if (frame.view != FarmView.SEEDS) return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    val slot = frame.freeSlot ?: return park()
                    if (slot !in 0..2 || (seedCountBefore ?: 0) <= 0) return park()
                    if (frame.selectedSlot == slot)
                        return issue(FarmCommand(FarmOperation.CONFIRM_SEED, waiting.plot, slot), now)
                    return issue(FarmCommand(FarmOperation.SELECT_FREE_SEED, waiting.plot, slot), now)
                }
                FarmOperation.OPEN_WATER -> {
                    if (frame.view != FarmView.WATER) return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    return issue(FarmCommand(FarmOperation.SELECT_WATER, waiting.plot), now)
                }
                FarmOperation.SELECT_WATER -> {
                    if(frame.view==FarmView.FIELD) {
                        // Some water/ad-skip paths complete directly from Choose. A returned
                        // field AND a changed target (ripe or water bubble gone) prove completion.
                        val targetState=frame.plots.getOrNull(waiting.plot ?: -1)
                        val completed=targetState==PlotState.RIPE ||
                            (targetState==PlotState.GROWING && waiting.plot !in frame.wateringPriorities)
                        if(!completed)return if(expired)park() else FarmCommand(FarmOperation.WAIT)
                        pending=null
                    } else {
                        if (frame.view != FarmView.WATER) return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                        return issue(FarmCommand(FarmOperation.CONFIRM_WATER, waiting.plot), now)
                    }
                }
                FarmOperation.CONFIRM_WATER -> {
                    if (frame.view != FarmView.FIELD) return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    if (waiting.plot in frame.wateringPriorities)
                        return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    pending = null
                }
                FarmOperation.SELECT_FREE_SEED -> {
                    if (frame.view != FarmView.SEEDS)
                        return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    // This transition follows our own verified tap on the free slot. Some animated
                    // glyphs corrupt both count OCR and the orange selection outline, so do not
                    // replace the explicitly chosen priority slot with a later noisy count read.
                    return issue(FarmCommand(FarmOperation.CONFIRM_SEED, waiting.plot, waiting.slot), now)
                }
                FarmOperation.CONFIRM_SEED -> {
                    if (frame.view != FarmView.FIELD)
                        return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    val plotState = frame.plots.getOrNull(waiting.plot!!)
                    val remaining = frame.freeSeeds
                    val before = seedCountBefore
                    // A timer/growing plot is direct proof. Animated Digimon may cover that timer;
                    // in that case accept UNKNOWN only together with a decreased seed counter.
                    // Exact OCR equality is intentionally avoided (live "15" can read as "12").
                    val plantedProof = plotState == PlotState.GROWING ||
                        (plotState == PlotState.UNKNOWN && remaining != null && before != null && remaining < before)
                    if (!plantedProof)
                        return if (expired) park() else FarmCommand(FarmOperation.WAIT)
                    planted++
                    plantedPlots += waiting.plot
                    pending = null
                    plantingPlot = null
                }
                else -> return park()
            }
        }
        if (frame.view != FarmView.FIELD || frame.plots.size != 6) return park()
        val ripe = frame.plots.indices.firstOrNull { frame.plots[it] == PlotState.RIPE && it !in frame.blockedPlots } ?: -1
        if (ripe >= 0) return issue(FarmCommand(FarmOperation.HARVEST, ripe), now)
        val empty = frame.plots.indices.firstOrNull { frame.plots[it] == PlotState.EMPTY && it !in frame.blockedPlots } ?: -1
        if (empty >= 0) {
            val seeds = frame.freeSeeds ?: return FarmCommand(FarmOperation.WAIT)
            if (seeds > 0) {
                seedCountBefore = seeds
                plantingPlot = empty
                return issue(FarmCommand(FarmOperation.OPEN_SEEDS, empty), now)
            }
        }
        if (frame.wateringEnabled) {
            val canUseWater = (frame.wateringCans ?: 0) > 0 || frame.adSkipPass
            val target = if (canUseWater) frame.wateringPriorities.entries
                .filter { (plot, _) -> frame.plots.getOrNull(plot) == PlotState.GROWING }
                .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { -it.key })?.key else null
            if (target != null) return issue(FarmCommand(FarmOperation.OPEN_WATER, target), now)
        }
        // Never tap an unknown plot and never call it complete. A later clear frame can reveal an
        // empty badge; treating UNKNOWN as finished left real empty fields unplanted on tall phones.
        val unresolvedUnknown = frame.plots.indices.any {
            frame.plots[it] == PlotState.UNKNOWN && it !in plantedPlots
        }
        if (unresolvedUnknown) return FarmCommand(FarmOperation.WAIT)
        return FarmCommand(FarmOperation.COMPLETE)
    }

    fun cancel() { terminal = FarmOperation.PARK; pending = null }

    private fun issue(command: FarmCommand, now: Long): FarmCommand {
        pending = command
        deadline = now + timeoutMillis
        issuedAt = now
        retries = 0
        return command
    }

    private fun park(): FarmCommand {
        terminal = FarmOperation.PARK
        pending = null
        return FarmCommand(FarmOperation.PARK)
    }
}

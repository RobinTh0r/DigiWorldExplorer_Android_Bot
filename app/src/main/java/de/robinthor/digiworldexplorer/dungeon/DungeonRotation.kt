package de.robinthor.digiworldexplorer.dungeon

import android.content.Context
import android.os.SystemClock

/** User-started dungeon pass. It is deliberately separate from the already-open VS/Tower loop. */
object DungeonRotationRequest {
    enum class Phase { IDLE, REQUESTED, OPENING_LIST, SURVEYING, RUNNING, PARKED, COMPLETE }

    @Volatile var phase = Phase.IDLE
        private set
    @Volatile var reason = ""
        private set
    @Volatile var requestedAt = 0L
        private set
    @Volatile var completedToday: Set<DungeonKey> = emptySet()
        private set

    @Synchronized fun start(context: Context) {
        DungeonRotationAnalyzer.reset()
        de.robinthor.digiworldexplorer.strategy.AutomationState.adSkipPassEnabled =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("ad_skip_pass", false)
        completedToday = DungeonDailyStore.snapshot(context).completed
        phase = Phase.REQUESTED
        reason = if (DungeonKey.APOCALYMON_WALL in completedToday)
            "Waiting for Home; Apocalymon already complete today" else "Waiting for verified Home"
        requestedAt = SystemClock.elapsedRealtime()
    }

    @Synchronized fun openingList() {
        if (phase == Phase.REQUESTED) {
            phase = Phase.OPENING_LIST
            reason = "Opening dungeon list"
        }
    }

    @Synchronized fun listVerified() {
        if (phase == Phase.REQUESTED || phase == Phase.OPENING_LIST) {
            phase = Phase.SURVEYING
            reason = "Dungeon list verified"
        }
    }

    @Synchronized fun park(why: String) { phase = Phase.PARKED; reason = why }
    @Synchronized fun complete() { phase = Phase.COMPLETE; reason = "Daily pass complete" }
    @Synchronized fun cancel() { phase = Phase.IDLE; reason = ""; requestedAt = 0L; completedToday = emptySet() }
    fun active() = phase in setOf(Phase.REQUESTED, Phase.OPENING_LIST, Phase.SURVEYING, Phase.RUNNING)
}

/** Safe defaults: visible counters remain authoritative; these are hard ceilings, never targets. */
object DungeonRotationPolicy {
    val budgets = mapOf(
        DungeonKey.APOCALYMON_WALL to DungeonBudget(attempts = 1),
        DungeonKey.DEMIDEVIMON to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.BAKEMON to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.DIGIFACTORY to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.NETWORK_DEFENSE to DungeonBudget(attempts = 4, adTickets = 2),
        DungeonKey.METAL_SEA to DungeonBudget(attempts = 4, adTickets = 2),
        // VS is opened once; its subsequent automatic battles stay in the existing VS module.
        DungeonKey.DAILY to DungeonBudget(attempts = 1),
    )

    fun adAllowed(key: DungeonKey, adSkipPass: Boolean) =
        adSkipPass && (budgets[key]?.adTickets ?: 0) > 0 && key != DungeonKey.APOCALYMON_WALL
}

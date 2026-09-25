package de.robinthor.digiworldexplorer.dungeon

enum class DungeonKey { APOCALYMON_WALL, DEMIDEVIMON, BAKEMON, DIGIFACTORY, NETWORK_DEFENSE, METAL_SEA, DAILY }
data class DungeonBudget(val attempts: Int = 0, val adTickets: Int = 0, val clearPreviousAfter: Int? = null) {
    init { require(attempts in 0..999 && adTickets in 0..99); require(clearPreviousAfter == null || clearPreviousAfter > 0) }
}
data class DungeonProgress(val attempts: Int = 0, val wins: Int = 0, val losses: Int = 0, val ads: Int = 0, val uncertain: Int = 0)
enum class BattleOutcome { WIN, LOSS, UNKNOWN }

/** One outstanding reservation globally: a repeated Start/result frame cannot spend twice. */
class DungeonBudgetLedger(private val budgets: Map<DungeonKey, DungeonBudget>) {
    private val progress = mutableMapOf<DungeonKey, DungeonProgress>()
    private var active: DungeonKey? = null

    fun snapshot(key: DungeonKey) = progress[key] ?: DungeonProgress()

    fun reserveStart(key: DungeonKey, observedTickets: Int?): Boolean {
        val budget = budgets[key] ?: return false
        val used = snapshot(key)
        if (active != null || (observedTickets ?: 0) <= 0 || used.attempts >= budget.attempts) return false
        // Reserve before sending Start. Unknown outcomes remain spent to prevent overshoot.
        active = key
        progress[key] = used.copy(attempts = used.attempts + 1)
        return true
    }

    fun recordResult(key: DungeonKey, outcome: BattleOutcome): Boolean {
        if (active != key) return false
        val used = snapshot(key)
        progress[key] = when (outcome) {
            BattleOutcome.WIN -> used.copy(wins = used.wins + 1)
            BattleOutcome.LOSS -> used.copy(losses = used.losses + 1)
            BattleOutcome.UNKNOWN -> used.copy(uncertain = used.uncertain + 1)
        }
        active = null
        return true
    }

    fun reserveAd(key: DungeonKey, observedAdsRemaining: Int?): Boolean {
        val budget = budgets[key] ?: return false
        val used = snapshot(key)
        if (active != null || (observedAdsRemaining ?: 0) <= 0 || used.ads >= budget.adTickets || used.attempts >= budget.attempts) return false
        progress[key] = used.copy(ads = used.ads + 1)
        return true
    }

    fun shouldClearPrevious(key: DungeonKey): Boolean {
        val threshold = budgets[key]?.clearPreviousAfter ?: return false
        return snapshot(key).attempts >= threshold
    }

    fun stop() { active?.let { recordResult(it, BattleOutcome.UNKNOWN) } }
}

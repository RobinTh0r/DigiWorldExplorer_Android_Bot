package de.robinthor.digiworldexplorer.dungeon

enum class DungeonRotationCommand { WAIT, SWIPE_TOP, SWIPE_BOTTOM, SELECT_CARD, COMPLETE, PARK }
data class DungeonRotationDecision(val command: DungeonRotationCommand, val card: DungeonCardReading? = null, val reason: String = "")

/** Pure card scheduler. It never maps an index unless the requested list end was proved. */
class DungeonRotationController(
    private val enabled: Set<DungeonKey>,
    completed: Set<DungeonKey>,
) {
    private val order = listOf(
        DungeonKey.APOCALYMON_WALL, DungeonKey.DEMIDEVIMON, DungeonKey.BAKEMON,
        DungeonKey.DIGIFACTORY, DungeonKey.NETWORK_DEFENSE, DungeonKey.METAL_SEA, DungeonKey.DAILY,
    )
    private val done = completed.toMutableSet()
    var current: DungeonKey? = null
        private set

    fun onList(reading: DungeonListReading, adsAllowed: (DungeonKey) -> Boolean): DungeonRotationDecision {
        if (current != null) return DungeonRotationDecision(DungeonRotationCommand.WAIT, reason = "card active")
        val next = order.firstOrNull { it in enabled && it !in done }
            ?: return DungeonRotationDecision(DungeonRotationCommand.COMPLETE, reason = "all enabled cards complete")
        val required = if (next in setOf(DungeonKey.APOCALYMON_WALL, DungeonKey.DEMIDEVIMON, DungeonKey.BAKEMON, DungeonKey.DIGIFACTORY)) DungeonListPosition.TOP else DungeonListPosition.BOTTOM
        if (reading.position == DungeonListPosition.NONE) return DungeonRotationDecision(DungeonRotationCommand.PARK, reason = "dungeon list not proved")
        if (reading.position != required) return DungeonRotationDecision(if (required == DungeonListPosition.TOP) DungeonRotationCommand.SWIPE_TOP else DungeonRotationCommand.SWIPE_BOTTOM)
        val card = reading.cards.firstOrNull { it.key == next }
            ?: return DungeonRotationDecision(DungeonRotationCommand.PARK, reason = "$next not visible at proved ${reading.position}")
        // The list counters for both daily actions are not authoritative: VS uses Destroy and
        // Apocalymon's stylised counter is frequently classified as zero. Open either card once
        // and let its verified modal decide whether an action is available.
        if (next !in DungeonPassPolicy.dailyLimited && card.tickets == CounterAvailability.ZERO && !adsAllowed(next)) {
            done += next
            return onList(reading, adsAllowed)
        }
        current = next
        return DungeonRotationDecision(DungeonRotationCommand.SELECT_CARD, card, "${card.tickets}")
    }

    fun finishCurrent() { current?.let { done += it }; current = null }
    fun releaseCurrent() { current = null }
    fun completed() = done.toSet()
}

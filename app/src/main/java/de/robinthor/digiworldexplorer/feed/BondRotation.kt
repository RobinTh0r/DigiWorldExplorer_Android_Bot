package de.robinthor.digiworldexplorer.feed

enum class BondStep { IDLE, OPEN, EXPAND, SELECT, RAISE, CONFIRM, VERIFY, HOME, COLLECT, REST, PARK }
data class BondCommand(val step: BondStep, val cell: Int? = null)

/** Visit all 15 visible partners; the original active partner is always the last target. */
class BondRotation {
    var step = BondStep.IDLE
        private set
    var original: Int? = null
        private set
    var visited = 0
        private set
    private var target: Int? = null
    private var deadline = 0L
    private var collectUntil = 0L
    private var nextBubbleArmed = false
    private var issuedAt = 0L
    private var retries = 0

    fun tick(home: Boolean, grid: PartnerGrid, feedBusy: Boolean, now: Long, bubbleVisible: Boolean = false,
        cycleReady: Boolean = true): BondCommand? {
        if (step == BondStep.PARK) return null
        if (step in setOf(BondStep.IDLE, BondStep.REST)) {
            if (!home || feedBusy || !cycleReady) return null
            if (step == BondStep.REST) {
                nextBubbleArmed = true
            }
            nextBubbleArmed = false
            original = null; visited = 0; target = null
            return issue(BondStep.OPEN, now)
        }
        if (step == BondStep.HOME && home) {
            collectUntil = now + 15_000; step = BondStep.COLLECT
        }
        if (step == BondStep.COLLECT) {
            // Farm may interrupt only here. Resume on Home without losing the original partner.
            if (!home || feedBusy || now < collectUntil) return null
            if (visited == 15) {
                step = BondStep.REST
                nextBubbleArmed = !bubbleVisible
                return null
            }
            return issue(BondStep.OPEN, now)
        }
        if (now >= deadline) { step = BondStep.PARK; return null }
        when (step) {
            BondStep.OPEN, BondStep.EXPAND -> {
                if (!grid.page) {
                    if (step == BondStep.OPEN && home && now - issuedAt >= 4_000 && retries < 2) {
                        retries++; issuedAt = now
                        return BondCommand(BondStep.OPEN)
                    }
                    return null
                }
                if (!grid.expanded) return if (step == BondStep.OPEN) issue(BondStep.EXPAND, now) else null
                if (grid.cells.size != 15 || grid.raised == null) return null
                if (original == null) original = grid.raised
                if (target != null && grid.raised != target) { step = BondStep.PARK; return null }
                target = (original!! + visited + 1) % 15
                return issue(BondStep.SELECT, now, target)
            }
            BondStep.SELECT -> if (grid.selected == target && grid.canRaise) return issue(BondStep.RAISE, now)
            BondStep.RAISE -> {
                if (grid.confirmation) return issue(BondStep.CONFIRM, now)
                if (grid.page && grid.selected == target && grid.canRaise && now - issuedAt >= 4_000 && retries < 2) {
                    retries++; issuedAt = now
                    return BondCommand(BondStep.RAISE)
                }
            }
            BondStep.CONFIRM -> if (grid.page && grid.raised == target) {
                visited++
                return issue(BondStep.HOME, now)
            }
            else -> Unit
        }
        return null
    }

    fun ownsFrame() = step !in setOf(BondStep.IDLE, BondStep.COLLECT, BondStep.REST)
    fun cancel() { step = BondStep.PARK }
    private fun issue(next: BondStep, now: Long, cell: Int? = null): BondCommand {
        step = next; deadline = now + 25_000; issuedAt = now; retries = 0
        return BondCommand(next, cell)
    }
}

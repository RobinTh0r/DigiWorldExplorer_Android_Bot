package de.robinthor.digiworldexplorer.feed

enum class BondStep { IDLE, OPEN, PARTNER_TAB, EXPAND, SELECT, RAISE, CONFIRM, VERIFY, HOME, COLLECT, REST, PARK }
enum class BondPauseReason { PARTNER_NOT_CONFIRMED, HOME_NOT_CONFIRMED, BUBBLE_NOT_FOUND, BUBBLE_COLLECTION_UNCONFIRMED }
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
    private var collectFastUntil = 0L
    private var nextBubbleArmed = false
    private var issuedAt = 0L
    private var retries = 0
    var collectStartedAt = 0L
        private set
    var bubbleSeenDuringCollect = false
        private set
    var pauseReason = BondPauseReason.PARTNER_NOT_CONFIRMED
        private set

    fun tick(home: Boolean, grid: PartnerGrid, feedBusy: Boolean, now: Long, bubbleVisible: Boolean = false,
        cycleReady: Boolean = true, bubbleCollected: Boolean = false): BondCommand? {
        if (step == BondStep.PARK) return null
        if (step in setOf(BondStep.IDLE, BondStep.REST)) {
            if (feedBusy || !cycleReady) return null
            if (step == BondStep.REST) {
                if (!home) return null
                nextBubbleArmed = true
            }
            nextBubbleArmed = false
            original = null; visited = 0; target = null
            pauseReason = BondPauseReason.PARTNER_NOT_CONFIRMED
            if (!grid.page) {
                if (!home) return null
                return issue(BondStep.OPEN, now)
            }
            // A stopped/restarted Co-Pilot can already be on the Partner page. Resume from
            // visible page evidence instead of blindly tapping the Home navigation button.
            step = BondStep.OPEN
            deadline = now + 25_000
            issuedAt = now
            retries = 0
        }
        if (step == BondStep.HOME && home) {
            collectStartedAt = now
            // A failed/restarting stage hides the bubble for roughly 3–10 seconds. Keep a clear
            // safety margin without stalling every partner for 90 seconds when no bubble exists.
            // The detector still requires verified Home and stable bubble evidence before tapping.
            // Scan immediately and frequently while the short-lived bubble can appear. Never tap
            // from this timer alone: FeedFrameAnalyzer still requires positive bubble evidence.
            collectFastUntil = now + 15_000
            collectUntil = now + 30_000
            deadline = collectUntil + 10_000
            bubbleSeenDuringCollect = false
            step = BondStep.COLLECT
        }
        if (step == BondStep.COLLECT) {
            // Farm may interrupt only here. Resume on Home without losing the original partner.
            if (bubbleVisible && !bubbleSeenDuringCollect) {
                collectUntil = maxOf(collectUntil, now + 20_000L)
                deadline = collectUntil + 10_000L
            }
            bubbleSeenDuringCollect = bubbleSeenDuringCollect || bubbleVisible
            // A visually confirmed tap is stronger evidence than the transient Home classifier:
            // the collection animation can cover the stable Home icons for several seconds.
            if (bubbleCollected && !feedBusy) {
                if (visited == 15) {
                    step = BondStep.REST
                    nextBubbleArmed = !bubbleVisible
                    return null
                }
                return issue(BondStep.OPEN, now)
            }
            if (!home || feedBusy) {
                if (now >= deadline) park(when {
                    bubbleSeenDuringCollect -> BondPauseReason.BUBBLE_COLLECTION_UNCONFIRMED
                    !home -> BondPauseReason.HOME_NOT_CONFIRMED
                    else -> BondPauseReason.BUBBLE_NOT_FOUND
                })
                return null
            }
            if (!bubbleCollected && now < collectUntil) return null
            if (!bubbleCollected && bubbleSeenDuringCollect) {
                park(BondPauseReason.BUBBLE_COLLECTION_UNCONFIRMED)
                return null
            }
            if (visited == 15) { step = BondStep.REST; nextBubbleArmed = !bubbleVisible; return null }
            return issue(BondStep.OPEN, now)
        }
        if (now >= deadline) {
            park(if (step == BondStep.HOME) BondPauseReason.HOME_NOT_CONFIRMED
                else BondPauseReason.PARTNER_NOT_CONFIRMED)
            return null
        }
        // Selecting Partner may reopen a roster which is already expanded. Observe that state
        // before deciding whether a '+' tap is needed; tapping its '-' would collapse the grid.
        if (step == BondStep.PARTNER_TAB && grid.page) step = BondStep.OPEN
        when (step) {
            BondStep.OPEN, BondStep.EXPAND -> {
                if (!grid.page) {
                    if (step == BondStep.OPEN && grid.digimonSection && grid.partnerTabTarget != null)
                        return issue(BondStep.PARTNER_TAB, now)
                    if (step == BondStep.OPEN && home && now - issuedAt >= 4_000 && retries < 2) {
                        retries++; issuedAt = now
                        return BondCommand(BondStep.OPEN)
                    }
                    return null
                }
                if (!grid.expanded) {
                    if (step == BondStep.OPEN) return issue(BondStep.EXPAND, now)
                    if (grid.expandTarget != null && now - issuedAt >= 1_500 && retries < 2) {
                        retries++; issuedAt = now
                        return BondCommand(BondStep.EXPAND)
                    }
                    return null
                }
                val visibleActive = grid.raised
                if (grid.cells.size != 15 || visibleActive == null) return null
                if (original == null) original = visibleActive
                // The yellow border/green check animate while the partner sheet opens. A sampled
                // frame can therefore briefly report the previous/wrong raised cell. Keep the
                // current command pending and let the existing deadline guard a genuinely wrong
                // screen instead of aborting the complete tour on that transient frame.
                if (target != null && visibleActive != target) return null
                target = (original!! + visited + 1) % 15
                return issue(BondStep.SELECT, now, target)
            }
            BondStep.PARTNER_TAB -> {
                if (grid.digimonSection && grid.partnerTabTarget != null && now - issuedAt >= 1_500 && retries < 3) {
                    retries++; issuedAt = now
                    return BondCommand(BondStep.PARTNER_TAB)
                }
            }
            BondStep.SELECT -> {
                if (grid.selected == target && grid.canRaise) return issue(BondStep.RAISE, now)
                if (grid.page && grid.expanded && now - issuedAt >= 2_000 && retries < 2) {
                    retries++; issuedAt = now
                    return BondCommand(BondStep.SELECT, target)
                }
            }
            BondStep.RAISE -> {
                // The confirmation dialog can appear and disappear between capture frames on
                // fast phones. A changed active-partner marker is definitive visual proof that
                // the switch completed; continue Home instead of waiting until the deadline.
                if (grid.page && grid.raised == target && !grid.canRaise) {
                    visited++
                    return issue(BondStep.HOME, now)
                }
                if (grid.confirmation) return issue(BondStep.CONFIRM, now)
                if (grid.page && grid.selected == target && grid.canRaise && now - issuedAt >= 4_000 && retries < 2) {
                    retries++; issuedAt = now
                    return BondCommand(BondStep.RAISE)
                }
            }
            BondStep.CONFIRM -> {
                if (grid.page && grid.raised == target) {
                    visited++
                    return issue(BondStep.HOME, now)
                }
                if (grid.confirmation && now - issuedAt >= 1_500 && retries < 2) {
                    retries++; issuedAt = now
                    return BondCommand(BondStep.CONFIRM)
                }
            }
            else -> Unit
        }
        return null
    }

    fun ownsFrame() = step !in setOf(BondStep.IDLE, BondStep.COLLECT, BondStep.REST)
    fun fastBubblePolling(now: Long) = step == BondStep.COLLECT && now < collectFastUntil
    fun cancel() { park(BondPauseReason.PARTNER_NOT_CONFIRMED) }
    private fun park(reason: BondPauseReason) { pauseReason = reason; step = BondStep.PARK }
    private fun issue(next: BondStep, now: Long, cell: Int? = null): BondCommand {
        step = next; deadline = now + 25_000; issuedAt = now; retries = 0
        return BondCommand(next, cell)
    }
}

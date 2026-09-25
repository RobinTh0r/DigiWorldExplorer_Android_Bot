package de.robinthor.digiworldexplorer.automation

enum class ScreenKind {
    UNKNOWN, CLEAN_HOME, FARM, SEED_DIALOG, WATER_DIALOG, PARTNER_GRID,
    DUNGEON_LIST, GEKKOMON_RUN, RESULT_DIALOG,
}

enum class GestureKind { TAP, SWIPE, BACK }

data class VerifiedAction(
    val code: String,
    val gesture: GestureKind,
    val allowedSources: Set<ScreenKind>,
    val expectedResults: Set<ScreenKind>,
    val timeoutMillis: Long = 3_000,
    val retryBudget: Int = 2,
) {
    init {
        require(code.isNotBlank())
        require(allowedSources.isNotEmpty() && expectedResults.isNotEmpty())
        require(ScreenKind.UNKNOWN !in allowedSources && ScreenKind.UNKNOWN !in expectedResults)
        require(timeoutMillis > 0 && retryBudget >= 0)
    }
}

enum class ActionDecision { DISPATCH, WAIT, VERIFIED, RETRY, PARK }

data class PendingVerifiedAction(
    val action: VerifiedAction,
    val attempt: Int,
    val deadlineMillis: Long,
)

/** Pure action lifecycle. Gesture execution stays in the Accessibility layer. */
class VerifiedActionController {
    var pending: PendingVerifiedAction? = null
        private set

    fun begin(action: VerifiedAction, source: ScreenKind, nowMillis: Long): ActionDecision {
        if (pending != null || source !in action.allowedSources) return ActionDecision.PARK
        pending = PendingVerifiedAction(action, attempt = 1, deadlineMillis = nowMillis + action.timeoutMillis)
        return ActionDecision.DISPATCH
    }

    fun observe(screen: ScreenKind, nowMillis: Long): ActionDecision {
        val current = pending ?: return ActionDecision.PARK
        if (screen in current.action.expectedResults) {
            pending = null
            return ActionDecision.VERIFIED
        }
        if (nowMillis < current.deadlineMillis) return ActionDecision.WAIT
        if (screen in current.action.allowedSources && current.attempt <= current.action.retryBudget) {
            pending = current.copy(attempt = current.attempt + 1, deadlineMillis = nowMillis + current.action.timeoutMillis)
            return ActionDecision.RETRY
        }
        pending = null
        return ActionDecision.PARK
    }

    fun cancel() { pending = null }
}

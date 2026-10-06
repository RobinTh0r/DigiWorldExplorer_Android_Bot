package de.robinthor.digiworldexplorer.automation

enum class EntryScreen { UNKNOWN, LOGIN_LOADING, LOGIN_READY, IDLE_CLAIM, IDLE_EMPTY, RESULT, NOTICE, HOME }
enum class EntryAction { WAIT, TOUCH_START, CLAIM_IDLE, CLAIM_AD, CLOSE_RESULT, CLOSE_IDLE, CLOSE_NOTICE, PARK }

/** Single-dispatch entry flow. Unknown dialogs cannot authorize a close or a retry. */
class GameEntryController {
    private var pending: EntryAction? = null
    private var deadline = 0L
    private var adBefore: Int? = null
    private var adClaims = 0
    private var awaitingAdProof = false
    private var parked = false
    private var issuedAt = 0L
    private var retries = 0
    private var resultReadyAt: Long? = null
    fun busy() = pending != null || parked

    fun tick(screen: EntryScreen, now: Long, adSkip: Boolean = false, adRemaining: Int? = null): EntryAction {
        if (parked) return EntryAction.PARK
        if (pending != null) {
            val done = when (pending) {
                EntryAction.TOUCH_START -> screen in setOf(EntryScreen.HOME, EntryScreen.IDLE_CLAIM, EntryScreen.IDLE_EMPTY, EntryScreen.NOTICE)
                EntryAction.CLAIM_IDLE, EntryAction.CLAIM_AD -> screen == EntryScreen.RESULT
                EntryAction.CLOSE_RESULT -> screen in setOf(EntryScreen.IDLE_CLAIM, EntryScreen.IDLE_EMPTY, EntryScreen.HOME)
                EntryAction.CLOSE_IDLE -> screen == EntryScreen.HOME
                EntryAction.CLOSE_NOTICE -> screen in setOf(EntryScreen.HOME, EntryScreen.IDLE_CLAIM, EntryScreen.IDLE_EMPTY)
                else -> false
            }
            if (!done) {
                if (now >= deadline) { parked = true; return EntryAction.PARK }
                // Unity occasionally consumes the accessibility callback without accepting the
                // visible result/receive tap. Retrying this idempotent close on the still proven
                // result screen is safer than waiting until the whole reward flow parks.
                val retryVisible = (pending == EntryAction.CLOSE_RESULT && screen == EntryScreen.RESULT) ||
                    (pending == EntryAction.TOUCH_START && screen == EntryScreen.LOGIN_READY) ||
                    (pending == EntryAction.CLOSE_NOTICE && screen == EntryScreen.NOTICE) ||
                    (pending == EntryAction.CLAIM_IDLE && screen == EntryScreen.IDLE_CLAIM) ||
                    (pending == EntryAction.CLOSE_IDLE && screen == EntryScreen.IDLE_EMPTY)
                val retryDelay = if (pending == EntryAction.TOUCH_START) 2_000L else 1_000L
                val retryLimit = if (pending == EntryAction.TOUCH_START) 4 else 2
                if (retryVisible && now - issuedAt >= retryDelay && retries < retryLimit) {
                    issuedAt = now
                    retries++
                    return pending!!
                }
                return EntryAction.WAIT
            }
            if (pending in setOf(EntryAction.CLAIM_IDLE, EntryAction.CLAIM_AD)) {
                val readyAt = resultReadyAt ?: (now + 1_000L).also { resultReadyAt = it }
                if (now < readyAt) return EntryAction.WAIT
                resultReadyAt = null
                awaitingAdProof = pending == EntryAction.CLAIM_AD
                return issue(EntryAction.CLOSE_RESULT, now)
            }
            pending = null
            resultReadyAt = null
        }
        if (screen == EntryScreen.HOME) { adClaims = 0; awaitingAdProof = false; return EntryAction.WAIT }
        if (awaitingAdProof) {
            if (adRemaining == null || adBefore == null || adRemaining != adBefore!! - 1) {
                parked = true; return EntryAction.PARK
            }
            awaitingAdProof = false
        }
        return when (screen) {
            EntryScreen.LOGIN_READY -> issue(EntryAction.TOUCH_START, now)
            EntryScreen.NOTICE -> issue(EntryAction.CLOSE_NOTICE, now)
            // RESULT reaches this controller only through the specific idle-reward detector,
            // so capture/service restarts may safely resume by closing it.
            EntryScreen.RESULT -> {
                val readyAt = resultReadyAt ?: (now + 1_000L).also { resultReadyAt = it }
                if (now < readyAt) EntryAction.WAIT
                else {
                    resultReadyAt = null
                    issue(EntryAction.CLOSE_RESULT, now)
                }
            }
            EntryScreen.IDLE_CLAIM, EntryScreen.IDLE_EMPTY -> {
                if (adSkip && adRemaining in 1..2 && adClaims < 2) {
                    adBefore = adRemaining; adClaims++
                    issue(EntryAction.CLAIM_AD, now)
                } else issue(if (screen == EntryScreen.IDLE_CLAIM) EntryAction.CLAIM_IDLE else EntryAction.CLOSE_IDLE, now)
            }
            else -> EntryAction.WAIT
        }
    }
    fun cancel() { parked = true }
    private fun issue(action: EntryAction, now: Long): EntryAction {
        pending = action
        issuedAt = now
        retries = 0
        deadline = now + if (action == EntryAction.TOUCH_START) 90_000 else 20_000
        return action
    }
}

package de.robinthor.digiworldexplorer.automation

enum class BotEyeState { OFF, ACTIVE, SEARCHING, UNKNOWN, ERROR }

/** Maps Director output to one unambiguous mascot-eye state. */
object BotEyeStateResolver {
    private val errorWords = listOf(
        "error", "failed", "paused", "parked", "unsafe", "blocked", "timeout",
        "could not", "not confirmed", "unavailable", "stuck",
    )
    private val searchWords = listOf(
        "search", "waiting", "checking", "opening", "detect", "scanning", "loading",
    )

    fun resolve(snapshot: DirectorSnapshot): BotEyeState {
        if (snapshot.state == "Automation off") return BotEyeState.OFF
        val action = snapshot.action.lowercase()
        if (snapshot.state == "Paused" || snapshot.screen == ObservedScreen.CAPTURE_BLOCKED ||
            errorWords.any(action::contains)) return BotEyeState.ERROR
        if (snapshot.state == "Watching" &&
            (snapshot.confidence > 0 || searchWords.any(action::contains))) return BotEyeState.SEARCHING
        if (snapshot.screen == ObservedScreen.UNKNOWN) return BotEyeState.UNKNOWN
        return BotEyeState.ACTIVE
    }
}

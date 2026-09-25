package de.robinthor.digiworldexplorer.automation

/** Stable, user-visible interpretation of the frame currently owned by the automation pipeline. */
enum class ObservedScreen(val label: String) {
    CAPTURE_BLOCKED("Capture unavailable"),
    MESSAGE("Game message"),
    HOME("Home"),
    NETWORK_DEFENSE("Network Defense"),
    WORLD_SEARCH("Digital World Search"),
    EXPLORE_MENU("Explore menu"),
    GEKKOMON_RUN("Gekkomon Run"),
    MEAT_FIELD("Meat Field"),
    MEAT_FIELD_DIALOG("Meat Field dialog"),
    DUNGEON_LIST("Dungeon list"),
    DUNGEON("Dungeon / Tower"),
    PARTNER_PAGE("Partner page"),
    PARTNER("Partner / Bond"),
    SUMMON("Summon"),
    UNKNOWN("Unknown screen"),
}

data class DirectorSnapshot(
    val screen: ObservedScreen = ObservedScreen.UNKNOWN,
    val state: String = "Watching",
    val action: String = "No action",
    val confidence: Int = 0,
    val bondCooldownMillis: Long = 0,
)

/**
 * Converts exclusive frame ownership into a debounced screen state. UNKNOWN is deliberately slow:
 * a brief animation must not erase a screen that was just proven by an owning analyzer.
 */
object ScreenDirector {
    private const val REQUIRED_MATCHES = 2
    private const val UNKNOWN_GRACE_FRAMES = 6

    private var current = ObservedScreen.UNKNOWN
    private var candidate = ObservedScreen.UNKNOWN
    private var candidateFrames = 0
    private var unknownFrames = 0
    private var action = "No action"
    private var automationEnabled = false

    @Synchronized
    fun observe(owner: FrameOwner, enabled: Boolean): DirectorSnapshot {
        return observeScreen(owner.screen(), enabled)
    }

    @Synchronized
    fun observeScreen(observed: ObservedScreen, enabled: Boolean): DirectorSnapshot {
        automationEnabled = enabled
        val previous = current
        if (observed == ObservedScreen.UNKNOWN) {
            unknownFrames++
            if (unknownFrames >= UNKNOWN_GRACE_FRAMES) {
                current = ObservedScreen.UNKNOWN
                candidate = ObservedScreen.UNKNOWN
                candidateFrames = 0
            }
        } else {
            unknownFrames = 0
            if (candidate == observed) candidateFrames++ else {
                candidate = observed
                candidateFrames = 1
            }
            if (candidateFrames >= REQUIRED_MATCHES) current = observed
        }
        if (current != previous) action = "No action"
        return snapshot()
    }

    @Synchronized
    fun noteAction(value: String, sourceScreen: ObservedScreen? = null) {
        // Status producers run in the same capture pipeline, but the visible screen is debounced.
        // Never let a late/false-positive analyzer overwrite the action belonging to another
        // already proven screen (for example VS/Tower while Meat Field is still current).
        if (sourceScreen != null && current != ObservedScreen.UNKNOWN &&
            sourceScreen != current &&
            !(sourceScreen == ObservedScreen.MEAT_FIELD && current == ObservedScreen.MEAT_FIELD_DIALOG) &&
            !(sourceScreen == ObservedScreen.MEAT_FIELD_DIALOG && current == ObservedScreen.MEAT_FIELD)
        ) return
        val clean = value.trim().replace(Regex("\\s+"), " ")
        if (clean.isNotEmpty()) action = clean.take(72)
    }

    @Synchronized fun snapshot() = DirectorSnapshot(
        screen = current,
        state = when {
            current == ObservedScreen.CAPTURE_BLOCKED -> "Paused"
            automationEnabled && current != ObservedScreen.UNKNOWN -> "Active"
            automationEnabled -> "Watching"
            else -> "Automation off"
        },
        action = action,
        confidence = when {
            current == ObservedScreen.UNKNOWN && candidate != ObservedScreen.UNKNOWN ->
                (candidateFrames.coerceAtMost(REQUIRED_MATCHES) * 100 / REQUIRED_MATCHES)
            current == ObservedScreen.UNKNOWN -> 0
            current == candidate -> (candidateFrames.coerceAtMost(REQUIRED_MATCHES) * 100 / REQUIRED_MATCHES)
            else -> 50
        },
        bondCooldownMillis = BondCycleTimer.remainingMillis(),
    )

    @Synchronized
    fun reset() {
        current = ObservedScreen.UNKNOWN
        candidate = ObservedScreen.UNKNOWN
        candidateFrames = 0
        unknownFrames = 0
        action = "No action"
        automationEnabled = false
    }

    private fun FrameOwner.screen() = when (this) {
        FrameOwner.CAPTURE_BLOCKED -> ObservedScreen.CAPTURE_BLOCKED
        FrameOwner.STAGE_FAILED -> ObservedScreen.MESSAGE
        FrameOwner.NETWORK_DEFENSE -> ObservedScreen.NETWORK_DEFENSE
        FrameOwner.WORLD_SEARCH -> ObservedScreen.WORLD_SEARCH
        FrameOwner.GEKKOMON_RUN -> ObservedScreen.GEKKOMON_RUN
        FrameOwner.FARM -> ObservedScreen.MEAT_FIELD
        FrameOwner.DUNGEON -> ObservedScreen.DUNGEON
        FrameOwner.BOND -> ObservedScreen.PARTNER
        FrameOwner.SUMMON -> ObservedScreen.SUMMON
        FrameOwner.NONE -> ObservedScreen.UNKNOWN
    }
}

package de.robinthor.digiworldexplorer.automation

/** The single subsystem allowed to own and act on a captured frame. */
enum class FrameOwner {
    CAPTURE_BLOCKED,
    GAME_ENTRY,
    STAGE_FAILED,
    NETWORK_DEFENSE,
    WORLD_SEARCH,
    GEKKOMON_RUN,
    FARM,
    DUNGEON,
    BOND,
    SUMMON,
    NONE,
}

data class FrameProbe(
    val owner: FrameOwner,
    val enabled: Boolean = true,
    val analyze: () -> Boolean,
)

/**
 * Resolves analyzer priority without allowing lower-priority probes to run after a match.
 *
 * Probes may contain stateful analyzers and gesture dispatch. Short-circuiting is therefore a
 * safety property, not merely an optimization: at most one analyzer can act on a given frame.
 */
object FrameOrchestrator {
    fun resolve(captureBlocked: Boolean, probes: List<FrameProbe>, activeSession: FrameProbe? = null): FrameOwner {
        if (captureBlocked) return FrameOwner.CAPTURE_BLOCKED
        // Ownership spans transitions, not just positively recognized individual images. A task
        // waiting for its postcondition must not hand loading/occluded frames to unrelated tasks.
        if (activeSession?.enabled == true) {
            activeSession.analyze()
            return activeSession.owner
        }
        for (probe in probes) {
            if (probe.enabled && probe.analyze()) return probe.owner
        }
        return FrameOwner.NONE
    }
}

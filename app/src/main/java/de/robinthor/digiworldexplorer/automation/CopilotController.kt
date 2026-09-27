package de.robinthor.digiworldexplorer.automation

enum class RunOwner { NONE, COPILOT, DUNGEON }
enum class CopilotPhase {
    STOPPED, ENTRY, WAIT_HOME, BOND, FIELD, REWARDS, DWS, RETURN_HOME, COOLDOWN,
    PAUSED_USER, PAUSED_CAPTURE, PARKED_ERROR, RECOVERY_REQUIRED,
}
enum class CopilotCommand { NONE, HANDLE_ENTRY, START_BOND, START_FIELD, OPEN_REWARDS, START_DWS, RETURN_HOME, WAIT }

data class CopilotOptions(val field: Boolean = true, val rewards: Boolean = true, val dws: Boolean = false)
data class CopilotState(
    val runId: Long = 0,
    val phase: CopilotPhase = CopilotPhase.STOPPED,
    val nextEligibleAtEpochMillis: Long = 0,
    val lastStablePhase: CopilotPhase = CopilotPhase.STOPPED,
    val pauseReason: String = "",
)

/** Pure coordinator. Screen analyzers execute commands; this class only authorizes transitions. */
class CopilotController(initial: CopilotState = CopilotState()) {
    var state = initial
        private set

    fun start(nowEpoch: Long, entryVisible: Boolean, homeVisible: Boolean): CopilotCommand {
        if (state.phase == CopilotPhase.RECOVERY_REQUIRED) return CopilotCommand.NONE
        if (state.phase == CopilotPhase.PAUSED_USER) state = state.copy(phase = state.lastStablePhase, pauseReason = "")
        if (state.nextEligibleAtEpochMillis > nowEpoch) {
            state = state.copy(phase = CopilotPhase.COOLDOWN, lastStablePhase = CopilotPhase.COOLDOWN)
            return CopilotCommand.WAIT
        }
        state = state.copy(
            runId = state.runId + 1,
            phase = if (entryVisible) CopilotPhase.ENTRY else if (homeVisible) CopilotPhase.BOND else CopilotPhase.WAIT_HOME,
            lastStablePhase = if (entryVisible) CopilotPhase.ENTRY else CopilotPhase.WAIT_HOME,
            pauseReason = "",
        )
        return when (state.phase) {
            CopilotPhase.ENTRY -> CopilotCommand.HANDLE_ENTRY
            CopilotPhase.BOND -> CopilotCommand.START_BOND
            else -> CopilotCommand.WAIT
        }
    }

    fun onHome(nowEpoch: Long): CopilotCommand = when (state.phase) {
        CopilotPhase.ENTRY, CopilotPhase.WAIT_HOME -> {
            if (state.nextEligibleAtEpochMillis > nowEpoch) {
                state = state.copy(phase = CopilotPhase.COOLDOWN, lastStablePhase = CopilotPhase.COOLDOWN)
                CopilotCommand.WAIT
            } else {
                state = state.copy(phase = CopilotPhase.BOND, lastStablePhase = CopilotPhase.BOND)
                CopilotCommand.START_BOND
            }
        }
        CopilotPhase.RETURN_HOME -> {
            state = state.copy(phase = CopilotPhase.COOLDOWN, lastStablePhase = CopilotPhase.COOLDOWN)
            CopilotCommand.WAIT
        }
        CopilotPhase.COOLDOWN -> if (state.nextEligibleAtEpochMillis <= nowEpoch) {
            state = state.copy(phase = CopilotPhase.BOND, lastStablePhase = CopilotPhase.BOND)
            CopilotCommand.START_BOND
        } else CopilotCommand.WAIT
        else -> CopilotCommand.NONE
    }

    fun bondCompleted(nowEpoch: Long, options: CopilotOptions): CopilotCommand {
        require(state.phase == CopilotPhase.BOND)
        val next = when { options.field -> CopilotPhase.FIELD; options.rewards -> CopilotPhase.REWARDS; options.dws -> CopilotPhase.DWS; else -> CopilotPhase.RETURN_HOME }
        state = state.copy(phase = next, lastStablePhase = next,
            nextEligibleAtEpochMillis = nowEpoch + BondCycleTimer.COOLDOWN_MILLIS)
        return command(next)
    }

    fun fieldCompleted(options: CopilotOptions): CopilotCommand {
        require(state.phase == CopilotPhase.FIELD)
        val next = when { options.rewards -> CopilotPhase.REWARDS; options.dws -> CopilotPhase.DWS; else -> CopilotPhase.RETURN_HOME }
        state = state.copy(phase = next, lastStablePhase = next)
        return command(next)
    }

    fun rewardsCompleted(options: CopilotOptions): CopilotCommand {
        require(state.phase == CopilotPhase.REWARDS)
        val next = if (options.dws) CopilotPhase.DWS else CopilotPhase.RETURN_HOME
        state = state.copy(phase = next, lastStablePhase = next)
        return command(next)
    }

    fun dwsCompleted(): CopilotCommand {
        require(state.phase == CopilotPhase.DWS)
        state = state.copy(phase = CopilotPhase.RETURN_HOME, lastStablePhase = CopilotPhase.RETURN_HOME)
        return CopilotCommand.RETURN_HOME
    }

    fun pause(reason: String, capture: Boolean = false) {
        if (state.phase in setOf(CopilotPhase.STOPPED, CopilotPhase.PAUSED_USER, CopilotPhase.PAUSED_CAPTURE)) return
        state = state.copy(lastStablePhase = state.phase,
            phase = if (capture) CopilotPhase.PAUSED_CAPTURE else CopilotPhase.PAUSED_USER, pauseReason = reason)
    }

    fun requireRecovery(reason: String) {
        state = state.copy(lastStablePhase = state.phase, phase = CopilotPhase.RECOVERY_REQUIRED, pauseReason = reason)
    }

    fun stop() { state = state.copy(phase = CopilotPhase.STOPPED, pauseReason = "") }

    private fun command(phase: CopilotPhase) = when (phase) {
        CopilotPhase.FIELD -> CopilotCommand.START_FIELD
        CopilotPhase.REWARDS -> CopilotCommand.OPEN_REWARDS
        CopilotPhase.DWS -> CopilotCommand.START_DWS
        CopilotPhase.RETURN_HOME -> CopilotCommand.RETURN_HOME
        else -> CopilotCommand.NONE
    }
}

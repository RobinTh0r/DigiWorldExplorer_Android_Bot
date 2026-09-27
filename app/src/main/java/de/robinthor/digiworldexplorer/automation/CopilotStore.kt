package de.robinthor.digiworldexplorer.automation

import android.content.Context

object CopilotStore {
    private const val SCHEMA = 1
    private const val PREFS = "digi_copilot_state"

    fun load(context: Context): CopilotState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getInt("schema", 0) != SCHEMA) return CopilotState()
        val phase = enumValueOrNull<CopilotPhase>(p.getString("phase", null)) ?: return CopilotState()
        val stable = enumValueOrNull<CopilotPhase>(p.getString("stable", null)) ?: CopilotPhase.STOPPED
        val stored = CopilotState(p.getLong("run_id", 0), phase, p.getLong("next_epoch", 0), stable, p.getString("reason", "").orEmpty())
        // Never resume a gesture-bearing phase after process death. Cooldown is passive and safe.
        return if (phase in setOf(CopilotPhase.STOPPED, CopilotPhase.COOLDOWN, CopilotPhase.PAUSED_USER)) stored
        else stored.copy(phase = CopilotPhase.RECOVERY_REQUIRED, lastStablePhase = phase,
            pauseReason = "Interrupted run requires confirmation")
    }

    fun save(context: Context, state: CopilotState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("schema", SCHEMA).putLong("run_id", state.runId)
            .putString("phase", state.phase.name).putString("stable", state.lastStablePhase.name)
            .putLong("next_epoch", state.nextEligibleAtEpochMillis).putString("reason", state.pauseReason)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
}

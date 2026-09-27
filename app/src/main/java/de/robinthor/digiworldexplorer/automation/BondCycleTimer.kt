package de.robinthor.digiworldexplorer.automation

/** Global Bond -> Farm -> Home cycle clock. A capture reset must not restart the cooldown. */
object BondCycleTimer {
    const val COOLDOWN_MILLIS = 20 * 60 * 1000L
    @Volatile private var awaitingFarm = false
    @Volatile private var cooldownUntil = 0L
    private var preferences: android.content.SharedPreferences? = null

    @Synchronized fun initialize(context: android.content.Context) {
        if (preferences != null) return
        val prefs = context.getSharedPreferences("bond_cycle_clock", android.content.Context.MODE_PRIVATE)
        preferences = prefs
        val remaining = restoredRemaining(prefs.getLong("due_epoch_ms", 0L), System.currentTimeMillis())
        cooldownUntil = android.os.SystemClock.elapsedRealtime() + remaining
        // Resume only the coarse, visually re-verified Farm/Home handoff. Older installations
        // have no marker; a live cooldown is treated as one safe recovery visit exactly once.
        awaitingFarm = if (prefs.contains("awaiting_farm"))
            prefs.getBoolean("awaiting_farm", false) else remaining > 0L
    }

    fun bondCompleted(now: Long = runCatching { android.os.SystemClock.elapsedRealtime() }
        .getOrElse { System.nanoTime() / 1_000_000L }) {
        awaitingFarm = true
        cooldownUntil = now + COOLDOWN_MILLIS
        preferences?.edit()
            ?.putLong("due_epoch_ms", System.currentTimeMillis() + COOLDOWN_MILLIS)
            ?.putBoolean("awaiting_farm", true)
            ?.apply()
    }
    fun farmReturnedHome(now: Long) {
        if (awaitingFarm) {
            awaitingFarm = false
            preferences?.edit()?.putBoolean("awaiting_farm", false)?.apply()
            // Farm/rewards run inside the Bond cooldown, never restart its deadline.
        }
    }
    /** Explicit Co-Pilot restart during cooldown rechecks the safe Farm/Home tail. */
    fun requestFarmRecovery() {
        if (remainingMillis() > 0L) {
            awaitingFarm = true
            preferences?.edit()?.putBoolean("awaiting_farm", true)?.apply()
        }
    }
    fun canStartBond(now: Long) = !awaitingFarm && now >= cooldownUntil
    fun remainingMillis(now: Long) = (cooldownUntil - now).coerceAtLeast(0L)
    fun remainingMillis() = remainingMillis(runCatching { android.os.SystemClock.elapsedRealtime() }
        .getOrElse { System.nanoTime() / 1_000_000L })
    fun awaitingFarm() = awaitingFarm
    internal fun restoredRemaining(dueEpoch: Long, nowEpoch: Long): Long =
        if (dueEpoch <= nowEpoch) 0L else (dueEpoch - nowEpoch).coerceAtMost(COOLDOWN_MILLIS)
    internal fun resetForTest() { awaitingFarm = false; cooldownUntil = 0L }
}

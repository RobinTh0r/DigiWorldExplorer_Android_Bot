package de.robinthor.digiworldexplorer.automation

/** Global Bond -> Farm -> Home cycle clock. A capture reset must not restart the cooldown. */
object BondCycleTimer {
    const val COOLDOWN_MILLIS = 20 * 60 * 1000L
    @Volatile private var awaitingFarm = false
    @Volatile private var cooldownUntil = 0L

    fun bondCompleted() { awaitingFarm = true }
    fun farmReturnedHome(now: Long) {
        if (awaitingFarm) {
            awaitingFarm = false
            cooldownUntil = now + COOLDOWN_MILLIS
        }
    }
    fun canStartBond(now: Long) = !awaitingFarm && now >= cooldownUntil
    fun remainingMillis(now: Long) = (cooldownUntil - now).coerceAtLeast(0L)
    fun remainingMillis() = remainingMillis(runCatching { android.os.SystemClock.elapsedRealtime() }
        .getOrElse { System.nanoTime() / 1_000_000L })
    fun awaitingFarm() = awaitingFarm
    internal fun resetForTest() { awaitingFarm = false; cooldownUntil = 0L }
}

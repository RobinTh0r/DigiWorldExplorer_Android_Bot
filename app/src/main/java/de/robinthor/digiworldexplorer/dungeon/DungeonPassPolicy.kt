package de.robinthor.digiworldexplorer.dungeon

/** Daily reporting is not a lock for repeatable dungeons. Only these two actions are daily. */
object DungeonPassPolicy {
    val dailyLimited = setOf(DungeonKey.APOCALYMON_WALL, DungeonKey.DAILY)
    fun locked(snapshot: DungeonDailySnapshot): Set<DungeonKey> = dailyLimited.filterTo(mutableSetOf()) {
        it in snapshot.completed || (snapshot.progress[it]?.attempts ?: 0) > 0
    }
}

/** Per-user-start reservations. Recreating a pass never changes the persistent daily ledger. */
class DungeonPassUsage {
    private val attempts = mutableMapOf<DungeonKey, Int>()
    private val ads = mutableMapOf<DungeonKey, Int>()
    fun attempts(key: DungeonKey) = attempts[key] ?: 0
    fun ads(key: DungeonKey) = ads[key] ?: 0
    fun reserveAttempt(key: DungeonKey) { attempts[key] = attempts(key) + 1 }
    fun reserveAd(key: DungeonKey) { ads[key] = ads(key) + 1 }
    fun limit(key: DungeonKey, normal: Int) = if (key in DungeonPassPolicy.dailyLimited) 1 else normal + ads(key)
}

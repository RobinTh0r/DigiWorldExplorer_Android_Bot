package de.robinthor.digiworldexplorer.dungeon

import android.content.Context
import de.robinthor.digiworldexplorer.automation.DailyResetClock
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class DungeonDailyProgress(val attempts: Int = 0, val wins: Int = 0, val losses: Int = 0, val ads: Int = 0, val skips: Int = 0, val unknown: Int = 0)
data class DungeonDailySnapshot(val gameDay: LocalDate, val completed: Set<DungeonKey> = emptySet(), val nextReset: ZonedDateTime, val progress: Map<DungeonKey, DungeonDailyProgress> = emptyMap()) {
    fun isComplete(key: DungeonKey) = key in completed
}

/** Persistent dungeon day. Europe/Berlin keeps 08:00 aligned through summer/winter time. */
object DungeonDailyStore {
    private val berlin = ZoneId.of("Europe/Berlin")
    private const val PREFS = "dungeon_daily"
    private const val DAY = "game_day"
    private const val COMPLETED = "completed_mask"
    private fun progressKey(key: DungeonKey, field: String) = "${key.name.lowercase()}_$field"

    fun snapshot(context: Context, clock: Clock = Clock.system(berlin)): DungeonDailySnapshot {
        val now = ZonedDateTime.now(clock).withZoneSameInstant(berlin)
        val gameDay = DailyResetClock.gameDay(now)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getInt("executor_schema", 0) < 2) {
            // The earlier executor counted ticket grants as battles and marked cards done
            // while their modal was still opening. Archive those unreliable counters once.
            val old = p.all.toString()
            val oldDay = p.getString(DAY, null)
            val apoc = p.getInt(COMPLETED, 0) and (1 shl DungeonKey.APOCALYMON_WALL.ordinal)
            p.edit().clear().putInt("executor_schema", 2).putString("legacy_snapshot", old)
                .putString(DAY, oldDay).putInt(COMPLETED, apoc).commit()
        }
        if (p.getInt("executor_schema", 0) < 3) {
            // Builds before 5.0.0 could mark VS complete from its zero ticket label without
            // opening the card or pressing Destroy. Reopen VS once for the current game day;
            // all correctly completed dungeon cards remain untouched.
            val dailyBit = 1 shl DungeonKey.DAILY.ordinal
            p.edit()
                .putInt("executor_schema", 3)
                .putInt(COMPLETED, p.getInt(COMPLETED, 0) and dailyBit.inv())
                .remove(progressKey(DungeonKey.DAILY, "attempts"))
                .remove(progressKey(DungeonKey.DAILY, "wins"))
                .remove(progressKey(DungeonKey.DAILY, "losses"))
                .remove(progressKey(DungeonKey.DAILY, "ads"))
                .remove(progressKey(DungeonKey.DAILY, "skips"))
                .remove(progressKey(DungeonKey.DAILY, "unknown"))
                .commit()
        }
        val sameDay = p.getString(DAY, null) == gameDay.toString()
        val mask = if (sameDay) p.getInt(COMPLETED, 0) else {
            p.edit().clear().putInt("executor_schema", 3).putString(DAY, gameDay.toString()).putInt(COMPLETED, 0).apply(); 0
        }
        val progress = if (!sameDay) emptyMap() else DungeonKey.entries.associateWith { key -> DungeonDailyProgress(
            attempts = p.getInt(progressKey(key, "attempts"), 0), wins = p.getInt(progressKey(key, "wins"), 0),
            losses = p.getInt(progressKey(key, "losses"), 0), ads = p.getInt(progressKey(key, "ads"), 0),
            skips = p.getInt(progressKey(key, "skips"), 0), unknown = p.getInt(progressKey(key, "unknown"), 0),
        ) }.filterValues { it != DungeonDailyProgress() }
        return DungeonDailySnapshot(
            gameDay,
            DungeonKey.entries.filterTo(mutableSetOf()) { mask and (1 shl it.ordinal) != 0 },
            DailyResetClock.nextReset(now),
            progress,
        )
    }

    fun record(context: Context, key: DungeonKey, transform: (DungeonDailyProgress) -> DungeonDailyProgress) {
        val current = snapshot(context)
        val value = transform(current.progress[key] ?: DungeonDailyProgress())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(DAY, current.gameDay.toString())
            .putInt(progressKey(key, "attempts"), value.attempts).putInt(progressKey(key, "wins"), value.wins)
            .putInt(progressKey(key, "losses"), value.losses).putInt(progressKey(key, "ads"), value.ads)
            .putInt(progressKey(key, "skips"), value.skips).putInt(progressKey(key, "unknown"), value.unknown).apply()
    }

    fun markComplete(context: Context, key: DungeonKey, clock: Clock = Clock.system(berlin)) {
        val current = snapshot(context, clock)
        val mask = (current.completed + key).fold(0) { value, item -> value or (1 shl item.ordinal) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(DAY, current.gameDay.toString()).putInt(COMPLETED, mask).apply()
    }

    fun reopen(context: Context, key: DungeonKey, clock: Clock = Clock.system(berlin)) {
        val current = snapshot(context, clock)
        val mask = (current.completed - key).fold(0) { value, item -> value or (1 shl item.ordinal) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(DAY, current.gameDay.toString()).putInt(COMPLETED, mask).apply()
    }

    fun millisUntilReset(context: Context, clock: Clock = Clock.system(berlin)): Long {
        val now = ZonedDateTime.now(clock).withZoneSameInstant(berlin)
        return java.time.Duration.between(now, snapshot(context, clock).nextReset).toMillis().coerceAtLeast(0)
    }
}

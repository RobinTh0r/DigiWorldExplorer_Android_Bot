package de.robinthor.digiworldexplorer.automation

import android.content.Context
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

/** Remembers that the optional idle Ad-Skip check was exhausted for the current 08:00 game day. */
object EntryDailyStore {
    private const val PREFS = "entry_daily"
    private const val DAY = "game_day"
    private const val ADS_DONE = "idle_ads_done"
    private val berlin = ZoneId.of("Europe/Berlin")

    fun adsDone(context: Context, clock: Clock = Clock.system(berlin)): Boolean {
        val day = DailyResetClock.gameDay(ZonedDateTime.now(clock).withZoneSameInstant(berlin)).toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(DAY, null) != day) {
            prefs.edit().clear().putString(DAY, day).apply()
            return false
        }
        return prefs.getBoolean(ADS_DONE, false)
    }

    fun markAdsDone(context: Context, clock: Clock = Clock.system(berlin)) {
        val day = DailyResetClock.gameDay(ZonedDateTime.now(clock).withZoneSameInstant(berlin)).toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(DAY, day).putBoolean(ADS_DONE, true).apply()
    }
}

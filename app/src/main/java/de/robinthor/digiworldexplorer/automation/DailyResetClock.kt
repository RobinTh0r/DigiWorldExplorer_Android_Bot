package de.robinthor.digiworldexplorer.automation

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** DigiWorld's local game day changes at 08:00, including DST transitions. */
object DailyResetClock {
    private val RESET_TIME: LocalTime = LocalTime.of(8, 0)

    fun gameDay(now: ZonedDateTime): LocalDate =
        if (now.toLocalTime().isBefore(RESET_TIME)) now.toLocalDate().minusDays(1) else now.toLocalDate()

    fun nextReset(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(RESET_TIME).atZone(now.zone)
        return if (now.isBefore(today)) today else now.toLocalDate().plusDays(1).atTime(RESET_TIME).atZone(now.zone)
    }
}

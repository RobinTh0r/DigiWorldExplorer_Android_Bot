package de.robinthor.digiworldexplorer.automation

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyResetClockTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test fun `before eight belongs to previous game day`() {
        val now = ZonedDateTime.of(2026, 9, 24, 7, 59, 59, 0, berlin)
        assertEquals(LocalDate.of(2026, 9, 23), DailyResetClock.gameDay(now))
    }

    @Test fun `at eight starts new game day`() {
        val now = ZonedDateTime.of(2026, 9, 24, 8, 0, 0, 0, berlin)
        assertEquals(LocalDate.of(2026, 9, 24), DailyResetClock.gameDay(now))
    }

    @Test fun `next reset remains eight across daylight saving transition`() {
        val beforeDstEnd = ZonedDateTime.of(2026, 10, 24, 12, 0, 0, 0, berlin)
        val reset = DailyResetClock.nextReset(beforeDstEnd)
        assertEquals(8, reset.hour)
        assertEquals(LocalDate.of(2026, 10, 25), reset.toLocalDate())
        assertEquals(berlin, reset.zone)
    }
}

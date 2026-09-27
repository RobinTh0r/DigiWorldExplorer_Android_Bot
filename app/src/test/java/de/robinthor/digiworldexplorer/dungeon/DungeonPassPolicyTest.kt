package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DungeonPassPolicyTest {
    private val day = LocalDate.of(2026, 9, 26)
    private fun snapshot(done: Set<DungeonKey>, progress: Map<DungeonKey, DungeonDailyProgress> = emptyMap()) =
        DungeonDailySnapshot(day, done, day.plusDays(1).atTime(8, 0).atZone(ZoneId.of("Europe/Berlin")), progress)

    @Test fun `second manual pass rechecks regular cards without reopening daily actions`() {
        val locked = DungeonPassPolicy.locked(snapshot(DungeonKey.entries.toSet()))
        assertEquals(setOf(DungeonKey.APOCALYMON_WALL, DungeonKey.DAILY), locked)
        val settings = DungeonRotationSettings(normalAttempts = 3)
        assertEquals(DungeonBudget(5, 2), settings.budget(DungeonKey.BAKEMON, true, DungeonKey.entries.toSet()))
        assertEquals(DungeonBudget(), settings.budget(DungeonKey.DAILY, true, locked))
        val scheduler = DungeonRotationController(DungeonKey.entries.toSet(), locked)
        val card = DungeonCardReading(DungeonKey.DEMIDEVIMON, de.robinthor.digiworldexplorer.vision.NormalizedPoint(.5,.5), CounterAvailability.POSITIVE)
        assertEquals(DungeonKey.DEMIDEVIMON, scheduler.onList(DungeonListReading(DungeonListPosition.TOP, listOf(card))) { true }.card?.key)
    }

    @Test fun `reserved daily action stays locked after interrupted result`() {
        val locked = DungeonPassPolicy.locked(snapshot(emptySet(), mapOf(
            DungeonKey.APOCALYMON_WALL to DungeonDailyProgress(attempts = 1),
            DungeonKey.BAKEMON to DungeonDailyProgress(attempts = 5))))
        assertEquals(setOf(DungeonKey.APOCALYMON_WALL), locked)
    }

    @Test fun `fresh pass resets normal budget but does not manufacture ad grants`() {
        val previous = DungeonPassUsage()
        repeat(3) { previous.reserveAttempt(DungeonKey.BAKEMON) }
        previous.reserveAd(DungeonKey.BAKEMON)
        assertEquals(4, previous.limit(DungeonKey.BAKEMON, 3))
        val next = DungeonPassUsage()
        assertEquals(0, next.attempts(DungeonKey.BAKEMON))
        assertEquals(0, next.ads(DungeonKey.BAKEMON))
        assertEquals(3, next.limit(DungeonKey.BAKEMON, 3))
        assertEquals(1, next.limit(DungeonKey.APOCALYMON_WALL, 3))
    }

    @Test fun `difficult DemiDevimon and Bakemon cards get bounded fifteen start cap`() {
        assertEquals(15, DungeonRotationAnalyzer.attemptLimit(DungeonKey.DEMIDEVIMON, 3))
        assertEquals(15, DungeonRotationAnalyzer.attemptLimit(DungeonKey.BAKEMON, 3))
        assertEquals(3, DungeonRotationAnalyzer.attemptLimit(DungeonKey.DIGIFACTORY, 3))
    }
}

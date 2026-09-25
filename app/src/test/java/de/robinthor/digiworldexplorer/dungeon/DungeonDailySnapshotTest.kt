package de.robinthor.digiworldexplorer.dungeon

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DungeonDailySnapshotTest {
    @Test fun `completion membership is explicit per card`() {
        val zone = ZoneId.of("Europe/Berlin")
        val reset = LocalDate.of(2026, 9, 26).atTime(8, 0).atZone(zone)
        val state = DungeonDailySnapshot(LocalDate.of(2026, 9, 25), setOf(DungeonKey.APOCALYMON_WALL), reset)
        assertTrue(state.isComplete(DungeonKey.APOCALYMON_WALL))
        assertFalse(state.isComplete(DungeonKey.BAKEMON))
        assertEquals(8, state.nextReset.hour)
        assertEquals(zone, state.nextReset.zone)
    }
}

package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.*
import org.junit.Test

class DungeonRotationTest {
    @Test fun `apocalymon is capped at one and never uses ads`() {
        val budget = DungeonRotationPolicy.budgets.getValue(DungeonKey.APOCALYMON_WALL)
        assertEquals(1, budget.attempts)
        assertEquals(0, budget.adTickets)
        assertFalse(DungeonRotationPolicy.adAllowed(DungeonKey.APOCALYMON_WALL, true))
    }

    @Test fun `regular dungeon ads require global pass`() {
        assertFalse(DungeonRotationPolicy.adAllowed(DungeonKey.BAKEMON, false))
        assertTrue(DungeonRotationPolicy.adAllowed(DungeonKey.BAKEMON, true))
        assertEquals(2, DungeonRotationPolicy.budgets.getValue(DungeonKey.BAKEMON).adTickets)
    }

    @Test fun `vs handoff is a single entry`() {
        assertEquals(1, DungeonRotationPolicy.budgets.getValue(DungeonKey.DAILY).attempts)
        assertFalse(DungeonRotationPolicy.adAllowed(DungeonKey.DAILY, true))
    }
}

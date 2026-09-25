package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.assertEquals
import org.junit.Test

class DungeonSettingsTest {
    @Test fun `regular budget combines configured normal and gated ads`() {
        val settings = DungeonRotationSettings(normalAttempts = 2, useAdAttempts = true)
        assertEquals(DungeonBudget(2, 0), settings.budget(DungeonKey.BAKEMON, false))
        assertEquals(DungeonBudget(4, 2), settings.budget(DungeonKey.BAKEMON, true))
    }
    @Test fun `disabled card has zero budget`() {
        val settings = DungeonRotationSettings(enabledCards = setOf(DungeonKey.DEMIDEVIMON))
        assertEquals(DungeonBudget(), settings.budget(DungeonKey.METAL_SEA, true))
    }
    @Test fun `special cards ignore regular and ad counts`() {
        val settings = DungeonRotationSettings(normalAttempts = 2, useAdAttempts = true)
        assertEquals(DungeonBudget(1), settings.budget(DungeonKey.APOCALYMON_WALL, true))
        assertEquals(DungeonBudget(1), settings.budget(DungeonKey.DAILY, true))
    }
    @Test fun `completed card has zero budget until daily reset`() {
        val settings = DungeonRotationSettings()
        assertEquals(DungeonBudget(), settings.budget(DungeonKey.APOCALYMON_WALL, true, setOf(DungeonKey.APOCALYMON_WALL)))
    }
}

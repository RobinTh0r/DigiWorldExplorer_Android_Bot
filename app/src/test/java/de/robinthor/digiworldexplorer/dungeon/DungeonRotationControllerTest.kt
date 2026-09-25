package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import org.junit.Assert.*
import org.junit.Test

class DungeonRotationControllerTest {
    private fun card(key: DungeonKey, tickets: CounterAvailability = CounterAvailability.POSITIVE) = DungeonCardReading(key, NormalizedPoint(.5, .5), tickets)

    @Test fun `completed apocalymon is skipped and demi is selected from proved top`() {
        val c = DungeonRotationController(DungeonKey.entries.toSet(), setOf(DungeonKey.APOCALYMON_WALL))
        val result = c.onList(DungeonListReading(DungeonListPosition.TOP, listOf(card(DungeonKey.DEMIDEVIMON)))) { true }
        assertEquals(DungeonRotationCommand.SELECT_CARD, result.command)
        assertEquals(DungeonKey.DEMIDEVIMON, result.card?.key)
    }

    @Test fun `bottom card cannot be selected before bottom is proved`() {
        val completed = setOf(DungeonKey.APOCALYMON_WALL, DungeonKey.DEMIDEVIMON, DungeonKey.BAKEMON, DungeonKey.DIGIFACTORY)
        val c = DungeonRotationController(DungeonKey.entries.toSet(), completed)
        assertEquals(DungeonRotationCommand.SWIPE_BOTTOM, c.onList(DungeonListReading(DungeonListPosition.TOP, emptyList())) { true }.command)
    }

    @Test fun `zero card without ads is skipped`() {
        val c = DungeonRotationController(setOf(DungeonKey.DEMIDEVIMON), emptySet())
        val result = c.onList(DungeonListReading(DungeonListPosition.TOP, listOf(card(DungeonKey.DEMIDEVIMON, CounterAvailability.ZERO)))) { false }
        assertEquals(DungeonRotationCommand.COMPLETE, result.command)
    }

    @Test fun `daily VS card is opened even when its ticket counter is zero`() {
        val c = DungeonRotationController(setOf(DungeonKey.DAILY), emptySet())
        val result = c.onList(
            DungeonListReading(DungeonListPosition.BOTTOM, listOf(card(DungeonKey.DAILY, CounterAvailability.ZERO)))
        ) { false }
        assertEquals(DungeonRotationCommand.SELECT_CARD, result.command)
        assertEquals(DungeonKey.DAILY, result.card?.key)
    }
}

package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.*
import org.junit.Test

class DungeonBudgetTest {
    @Test fun `pending start and duplicate result cannot consume budget twice`() {
        val key = DungeonKey.BAKEMON
        val ledger = DungeonBudgetLedger(mapOf(key to DungeonBudget(attempts = 1)))
        assertTrue(ledger.reserveStart(key, 5))
        assertFalse(ledger.reserveStart(key, 5))
        assertTrue(ledger.recordResult(key, BattleOutcome.WIN))
        assertFalse(ledger.recordResult(key, BattleOutcome.WIN))
        assertFalse(ledger.reserveStart(key, 4))
        assertEquals(1, ledger.snapshot(key).wins)
    }

    @Test fun `unknown tickets and default ad permission never spend`() {
        val key = DungeonKey.METAL_SEA
        val ledger = DungeonBudgetLedger(mapOf(key to DungeonBudget(attempts = 3)))
        assertFalse(ledger.reserveStart(key, null))
        assertFalse(ledger.reserveAd(key, 10))
        assertTrue(ledger.reserveStart(key, 2))
        ledger.stop()
        assertEquals(1, ledger.snapshot(key).uncertain)
        assertEquals(1, ledger.snapshot(key).attempts)
    }
}

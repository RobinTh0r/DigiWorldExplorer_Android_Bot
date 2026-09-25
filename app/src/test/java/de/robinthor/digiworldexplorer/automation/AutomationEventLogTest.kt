package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationEventLogTest {
    @Test fun `ring keeps only newest two hundred events`() {
        AutomationEventLog.clear()
        repeat(205) { AutomationEventLog.record(AutomationEventKind.OWNER_CHANGED, "OWNER_$it", it.toLong()) }
        val result = AutomationEventLog.snapshot()
        assertEquals(200, result.size)
        assertEquals("OWNER_5", result.first().code)
        assertEquals("OWNER_204", result.last().code)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `diagnostic codes reject free form personal text`() {
        AutomationEventLog.record(AutomationEventKind.PARKED, "player name here", 0)
    }
}

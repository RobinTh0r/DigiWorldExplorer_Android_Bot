package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationModeTest {
    @Test fun `unknown preference safely falls back to semi auto`() {
        assertEquals(AutomationMode.SEMI_AUTO, AutomationMode.fromPreference("future-value"))
        assertEquals(AutomationMode.FULL_AUTOPILOT, AutomationMode.fromPreference("FULL_AUTOPILOT"))
    }

    @Test fun `semi auto cannot navigate from home`() {
        assertFalse(AutomationModePolicy.allows(AutomationMode.SEMI_AUTO, AutomationScreen.CLEAN_HOME, AutomationIntent.NAVIGATE_FROM_HOME))
        assertTrue(AutomationModePolicy.allows(AutomationMode.SEMI_AUTO, AutomationScreen.TASK_SCREEN, AutomationIntent.ACT_IN_OPEN_TASK))
    }

    @Test fun `full autopilot navigation requires clean home`() {
        assertTrue(AutomationModePolicy.allows(AutomationMode.FULL_AUTOPILOT, AutomationScreen.CLEAN_HOME, AutomationIntent.NAVIGATE_FROM_HOME))
        assertFalse(AutomationModePolicy.allows(AutomationMode.FULL_AUTOPILOT, AutomationScreen.UNKNOWN, AutomationIntent.NAVIGATE_FROM_HOME))
        assertFalse(AutomationModePolicy.allows(AutomationMode.FULL_AUTOPILOT, AutomationScreen.DIALOG, AutomationIntent.NAVIGATE_FROM_HOME))
    }
}

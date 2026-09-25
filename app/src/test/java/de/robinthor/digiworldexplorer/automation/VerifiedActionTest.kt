package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerifiedActionTest {
    private val action = VerifiedAction(
        code = "OPEN_FARM",
        gesture = GestureKind.TAP,
        allowedSources = setOf(ScreenKind.CLEAN_HOME),
        expectedResults = setOf(ScreenKind.FARM),
        timeoutMillis = 100,
        retryBudget = 2,
    )

    @Test fun `wrong source parks without scheduling gesture`() {
        val controller = VerifiedActionController()
        assertEquals(ActionDecision.PARK, controller.begin(action, ScreenKind.UNKNOWN, 0))
        assertNull(controller.pending)
    }

    @Test fun `expected screen verifies and clears pending action`() {
        val controller = VerifiedActionController()
        assertEquals(ActionDecision.DISPATCH, controller.begin(action, ScreenKind.CLEAN_HOME, 0))
        assertEquals(ActionDecision.VERIFIED, controller.observe(ScreenKind.FARM, 20))
        assertNull(controller.pending)
    }

    @Test fun `timeouts retry only within budget then park`() {
        val controller = VerifiedActionController()
        controller.begin(action, ScreenKind.CLEAN_HOME, 0)
        assertEquals(ActionDecision.RETRY, controller.observe(ScreenKind.CLEAN_HOME, 100))
        assertEquals(ActionDecision.RETRY, controller.observe(ScreenKind.CLEAN_HOME, 200))
        assertEquals(ActionDecision.PARK, controller.observe(ScreenKind.CLEAN_HOME, 300))
        assertNull(controller.pending)
    }

    @Test fun `unknown or unexpected dialog at timeout never repeats original tap`() {
        for (screen in listOf(ScreenKind.UNKNOWN, ScreenKind.WATER_DIALOG)) {
            val controller = VerifiedActionController()
            controller.begin(action, ScreenKind.CLEAN_HOME, 0)
            assertEquals(ActionDecision.PARK, controller.observe(screen, 100))
            assertNull(controller.pending)
        }
    }
}

package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class CopilotControllerTest {
    @Test fun fullCycleStartsDeadlineAtBondAndKeepsItThroughOtherWork() {
        val c = CopilotController()
        assertEquals(CopilotCommand.START_BOND, c.start(1_000, false, true))
        assertEquals(CopilotCommand.START_FIELD, c.bondCompleted(2_000, CopilotOptions(dws = true)))
        val deadline = c.state.nextEligibleAtEpochMillis
        assertEquals(CopilotCommand.OPEN_REWARDS, c.fieldCompleted(CopilotOptions(dws = true)))
        assertEquals(CopilotCommand.START_DWS, c.rewardsCompleted(CopilotOptions(dws = true)))
        assertEquals(CopilotCommand.RETURN_HOME, c.dwsCompleted())
        assertEquals(CopilotCommand.WAIT, c.onHome(50_000))
        assertEquals(deadline, c.state.nextEligibleAtEpochMillis)
    }

    @Test fun cooldownNeverStartsActionsBeforeDeadline() {
        val c = CopilotController(CopilotState(4, CopilotPhase.COOLDOWN, 20_000, CopilotPhase.COOLDOWN))
        assertEquals(CopilotCommand.WAIT, c.start(10_000, false, true))
        assertEquals(CopilotCommand.WAIT, c.onHome(19_999))
        assertEquals(CopilotCommand.START_BOND, c.onHome(20_000))
    }

    @Test fun disabledSubtasksLeadToVerifiedHomeBoundary() {
        val c = CopilotController()
        c.start(0, false, true)
        assertEquals(CopilotCommand.RETURN_HOME, c.bondCompleted(1, CopilotOptions(false, false, false)))
        assertEquals(CopilotCommand.WAIT, c.onHome(2))
        assertEquals(CopilotPhase.COOLDOWN, c.state.phase)
    }

    @Test fun capturePauseRetainsCheckpointWithoutDispatch() {
        val c = CopilotController()
        c.start(0, false, true)
        c.pause("capture stopped", capture = true)
        assertEquals(CopilotPhase.PAUSED_CAPTURE, c.state.phase)
        assertEquals(CopilotPhase.BOND, c.state.lastStablePhase)
        assertEquals(CopilotCommand.NONE, c.onHome(5_000))
    }
}

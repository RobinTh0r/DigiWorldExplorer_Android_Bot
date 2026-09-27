package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class GameEntryControllerTest {
    @Test fun loadingWaitsAndTouchStartIsNotRepeated() {
        val c = GameEntryController()
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.LOGIN_LOADING,0))
        assertEquals(EntryAction.TOUCH_START,c.tick(EntryScreen.LOGIN_READY,1))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.LOGIN_READY,2))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.UNKNOWN,80_000))
        assertEquals(EntryAction.PARK,c.tick(EntryScreen.UNKNOWN,90_001))
    }
    @Test fun normalIdleClaimResultEmptyAndHome() {
        val c = GameEntryController()
        assertEquals(EntryAction.CLAIM_IDLE,c.tick(EntryScreen.IDLE_CLAIM,0))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.IDLE_CLAIM,1))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.RESULT,2))
        assertEquals(EntryAction.CLOSE_RESULT,c.tick(EntryScreen.RESULT,1_002))
        assertEquals(EntryAction.CLOSE_IDLE,c.tick(EntryScreen.IDLE_EMPTY,1_003))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.HOME,1_004))
    }
    @Test fun idleResultCanResumeAfterCaptureRestartButUnknownIsNotClosed() {
        val resumed = GameEntryController()
        assertEquals(EntryAction.WAIT,resumed.tick(EntryScreen.RESULT,0))
        assertEquals(EntryAction.CLOSE_RESULT,resumed.tick(EntryScreen.RESULT,1_000))
        assertEquals(EntryAction.WAIT,GameEntryController().tick(EntryScreen.UNKNOWN,1))
    }
    @Test fun visibleResultRetriesReceiveTapWithoutUnboundedSpam() {
        val c = GameEntryController()
        assertEquals(EntryAction.WAIT, c.tick(EntryScreen.RESULT, 0))
        assertEquals(EntryAction.CLOSE_RESULT, c.tick(EntryScreen.RESULT, 1_000))
        assertEquals(EntryAction.CLOSE_RESULT, c.tick(EntryScreen.RESULT, 2_000))
        assertEquals(EntryAction.CLOSE_RESULT, c.tick(EntryScreen.RESULT, 3_000))
        assertEquals(EntryAction.WAIT, c.tick(EntryScreen.RESULT, 4_000))
        assertEquals(EntryAction.PARK, c.tick(EntryScreen.RESULT, 21_000))
    }
    @Test fun adNeedsPassAndCountThenVerifiedDecrement() {
        assertEquals(EntryAction.CLAIM_IDLE,GameEntryController().tick(EntryScreen.IDLE_CLAIM,0,false,2))
        assertEquals(EntryAction.CLAIM_IDLE,GameEntryController().tick(EntryScreen.IDLE_CLAIM,0,true,null))
        val c = GameEntryController()
        assertEquals(EntryAction.CLAIM_AD,c.tick(EntryScreen.IDLE_CLAIM,0,true,2))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.RESULT,1,true))
        assertEquals(EntryAction.CLOSE_RESULT,c.tick(EntryScreen.RESULT,1_001,true))
        assertEquals(EntryAction.CLAIM_AD,c.tick(EntryScreen.IDLE_CLAIM,1_002,true,1))
        assertEquals(EntryAction.WAIT,c.tick(EntryScreen.RESULT,1_003,true))
        assertEquals(EntryAction.CLOSE_RESULT,c.tick(EntryScreen.RESULT,2_003,true))
        assertEquals(EntryAction.CLAIM_IDLE,c.tick(EntryScreen.IDLE_CLAIM,2_004,true,0))
    }
    @Test fun unchangedAdCountParksInsteadOfSpendingAgain() {
        val c = GameEntryController()
        c.tick(EntryScreen.IDLE_CLAIM,0,true,2)
        c.tick(EntryScreen.RESULT,1,true)
        c.tick(EntryScreen.RESULT,1_001,true)
        assertEquals(EntryAction.PARK,c.tick(EntryScreen.IDLE_CLAIM,1_002,true,2))
    }
    @Test fun `notice after login is dismissed once and requires home proof`() {
        val c = GameEntryController()
        assertEquals(EntryAction.TOUCH_START, c.tick(EntryScreen.LOGIN_READY, 0))
        assertEquals(EntryAction.CLOSE_NOTICE, c.tick(EntryScreen.NOTICE, 1))
        assertEquals(EntryAction.WAIT, c.tick(EntryScreen.NOTICE, 2))
        assertEquals(EntryAction.WAIT, c.tick(EntryScreen.HOME, 3))
    }
}

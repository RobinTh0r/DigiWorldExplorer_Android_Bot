package de.robinthor.digiworldexplorer.feed

import org.junit.Assert.*
import org.junit.Test

class GrowthGuideRecoveryTest {
    @Test fun firstFrameBlocksButDoesNotTapUntilSecondFreshConfirmation() {
        val state=GrowthGuideRecovery();state.observe(true,0)
        assertTrue(state.visible);assertFalse(state.mayTap(0))
        state.observe(true,500);assertTrue(state.mayTap(500))
    }
    @Test fun disappearanceReleasesOwnershipAndResetsAttemptBudget() {
        val state=GrowthGuideRecovery();state.observe(true,0);state.observe(true,500);state.dispatched(500)
        state.observe(false,1000)
        assertFalse(state.visible);assertFalse(state.mayTap(1000));assertEquals(0,state.attempts)
    }
    @Test fun threeDeliveredAttemptsThenWaitWithoutEndlessClicks() {
        val state=GrowthGuideRecovery();state.observe(true,0);state.observe(true,500)
        for(t in listOf(500L,2000L,3500L)){state.observe(true,t);assertTrue(state.mayTap(t));state.dispatched(t);assertFalse(state.mayTap(t+100))}
        state.observe(true,5000);assertFalse(state.mayTap(5000));assertTrue(state.visible)
    }
    @Test fun rejectedInputIsNotAConsumedDismissalAttempt() {
        val state=GrowthGuideRecovery();state.observe(true,0);state.observe(true,500)
        state.dispatched(500);state.rejected();assertEquals(0,state.attempts)
        assertFalse(state.mayTap(600));state.observe(true,2000);assertTrue(state.mayTap(2000))
    }
    @Test fun staleObservationCannotAuthorizeOneFrameDismissal() {
        val state=GrowthGuideRecovery();state.observe(true,0);state.observe(true,500)
        state.observe(true,5000);assertFalse(state.mayTap(5000))
    }
}

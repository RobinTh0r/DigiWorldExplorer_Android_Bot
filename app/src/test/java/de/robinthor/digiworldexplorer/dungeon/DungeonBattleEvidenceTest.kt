package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.*
import org.junit.Test

class DungeonBattleEvidenceTest {
    @Test fun unknownReturnCountsOnceAndLateFailureCanStillResolveIt() {
        val state=DungeonBattleEvidence()
        state.begin()
        assertTrue(state.returnedWithoutResult())
        assertFalse(state.returnedWithoutResult())
        assertEquals(1,state.consecutiveUnconfirmed)
        assertTrue(state.result(BattleOutcome.LOSS))
        assertEquals(0,state.consecutiveUnconfirmed)
        assertEquals(1,state.consecutiveLosses)
    }
    @Test fun verifiedVictoryResetsUnknownReturnStreak() {
        val state=DungeonBattleEvidence()
        repeat(3) { state.begin();state.returnedWithoutResult() }
        assertEquals(3,state.consecutiveUnconfirmed)
        state.begin();assertTrue(state.result(BattleOutcome.WIN))
        assertEquals(0,state.consecutiveUnconfirmed)
    }
    @Test fun acceptedBattleCannotBeRestartedAsAnUnacceptedTap() {
        val state=DungeonBattleEvidence()
        assertFalse(state.canRetryStart(false))
        state.begin()
        assertTrue(state.canRetryStart(false))
        assertFalse(state.canRetryStart(true))
        state.result(BattleOutcome.LOSS)
        assertFalse(state.canRetryStart(false))
    }
    @Test fun repeatedLossFramesCountOnceAndAllowRetryDismissal() {
        val state=DungeonBattleEvidence()
        assertFalse(state.result(BattleOutcome.LOSS))
        state.begin()
        assertTrue(state.result(BattleOutcome.LOSS))
        assertFalse(state.result(BattleOutcome.LOSS))
        assertFalse(state.result(BattleOutcome.WIN))
        assertEquals(1,state.consecutiveLosses)
    }
    @Test fun returnedPanelWithoutResultDoesNotInventWinOrResetFailures() {
        val state=DungeonBattleEvidence()
        state.begin();state.result(BattleOutcome.LOSS)
        state.begin() // Entry return alone does not call result(WIN).
        assertEquals(1,state.consecutiveLosses)
        assertTrue(state.result(BattleOutcome.LOSS))
        assertEquals(2,state.consecutiveLosses)
    }
    @Test fun threeConsecutiveFailuresStopRetriesButRealRewardResetsStreak() {
        val state=DungeonBattleEvidence()
        repeat(3) { state.begin();assertTrue(state.result(BattleOutcome.LOSS)) }
        assertFalse(state.canStart(3))
        assertTrue(state.canStart(4))
        state.begin();assertTrue(state.result(BattleOutcome.WIN))
        assertTrue(state.canStart(3))
        assertEquals(0,state.consecutiveLosses)
    }
}

package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class BondClockRestoreTest {
    @Test fun restartKeepsOriginalDeadline() {
        assertEquals(900_000L, BondCycleTimer.restoredRemaining(2_200_000L, 1_300_000L))
    }
    @Test fun expiredOrMissingDeadlineIsReady() {
        assertEquals(0L, BondCycleTimer.restoredRemaining(0L, 1_300_000L))
        assertEquals(0L, BondCycleTimer.restoredRemaining(1_200_000L, 1_300_000L))
    }
    @Test fun BackwardClockCannotCreateAnUnboundedWait() {
        assertEquals(BondCycleTimer.COOLDOWN_MILLIS, BondCycleTimer.restoredRemaining(9_000_000L, 1L))
    }
}

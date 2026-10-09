package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.*
import org.junit.Test

class DungeonAdReturnPolicyTest {
    @Test fun fastGrantWithoutIntermediateFrameIsConfirmed() {
        for (kind in listOf("challenge", "network_challenge", "network_matching")) {
            assertTrue(DungeonAdReturnPolicy.ticketGranted(kind, 1))
            assertFalse(DungeonAdReturnPolicy.canRetry(kind, 1, false))
        }
    }

    @Test fun unchangedAdMayRetryButNeverOtherActions() {
        assertTrue(DungeonAdReturnPolicy.canRetry("ad", 1, false))
        assertFalse(DungeonAdReturnPolicy.canRetry("ad", 1, true))
        assertFalse(DungeonAdReturnPolicy.canRetry("ad", 0, false))
        assertFalse(DungeonAdReturnPolicy.canRetry("destroy", 1, false))
    }

    @Test fun zeroUnknownAndAdCountersAreNotTicketGrantProof() {
        assertFalse(DungeonAdReturnPolicy.ticketGranted("challenge", 0))
        assertFalse(DungeonAdReturnPolicy.ticketGranted("challenge", null))
        assertFalse(DungeonAdReturnPolicy.ticketGranted("ad", 1))
        assertFalse(DungeonAdReturnPolicy.ticketGranted(null, null))
    }
}

package de.robinthor.digiworldexplorer.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDefenseOwnershipTest {
    @Test fun `classic loop gives up but dungeon copilot finishes Diaboromon`() {
        assertTrue(NetworkDefenseFrameAnalyzer.shouldGiveUpAtFinalBoss(rotationOwned = false))
        assertFalse(NetworkDefenseFrameAnalyzer.shouldGiveUpAtFinalBoss(rotationOwned = true))
    }
}

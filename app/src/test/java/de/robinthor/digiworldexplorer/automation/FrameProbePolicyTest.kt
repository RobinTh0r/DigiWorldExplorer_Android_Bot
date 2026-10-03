package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameProbePolicyTest {
    @Test fun `active network session blocks generic title taps`() {
        assertFalse(FrameProbePolicy.allowGenericGameEntry(
            featureFrame = true,
            networkDefenseSessionActive = true,
            digiCopilotOwns = false,
            awaitingFarm = false,
            rewardSequenceActive = false,
        ))
    }

    @Test fun `generic entry remains available outside owned sessions`() {
        assertTrue(FrameProbePolicy.allowGenericGameEntry(
            featureFrame = true,
            networkDefenseSessionActive = false,
            digiCopilotOwns = false,
            awaitingFarm = false,
            rewardSequenceActive = false,
        ))
    }
}

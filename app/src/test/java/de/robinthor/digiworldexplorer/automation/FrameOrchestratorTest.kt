package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameOrchestratorTest {
    @Test fun captureFailureSkipsEveryProbe() {
        var called = false
        val owner = FrameOrchestrator.resolve(true, listOf(FrameProbe(FrameOwner.STAGE_FAILED) { called = true; true }))
        assertEquals(FrameOwner.CAPTURE_BLOCKED, owner)
        assertTrue(!called)
    }

    @Test fun firstMatchingProbeOwnsFrameAndShortCircuits() {
        val calls = mutableListOf<FrameOwner>()
        val owner = FrameOrchestrator.resolve(false, listOf(
            FrameProbe(FrameOwner.STAGE_FAILED) { calls += FrameOwner.STAGE_FAILED; false },
            FrameProbe(FrameOwner.NETWORK_DEFENSE) { calls += FrameOwner.NETWORK_DEFENSE; true },
            FrameProbe(FrameOwner.WORLD_SEARCH) { calls += FrameOwner.WORLD_SEARCH; true },
        ))
        assertEquals(FrameOwner.NETWORK_DEFENSE, owner)
        assertEquals(listOf(FrameOwner.STAGE_FAILED, FrameOwner.NETWORK_DEFENSE), calls)
    }

    @Test fun disabledProbeIsNotInvoked() {
        var disabledCalled = false
        val owner = FrameOrchestrator.resolve(false, listOf(
            FrameProbe(FrameOwner.STAGE_FAILED, enabled = false) { disabledCalled = true; true },
            FrameProbe(FrameOwner.SUMMON) { true },
        ))
        assertEquals(FrameOwner.SUMMON, owner)
        assertTrue(!disabledCalled)
    }

    @Test fun noneReturnedWhenNothingRecognizesFrame() {
        val owner = FrameOrchestrator.resolve(false, FrameOwner.entries.filter { it != FrameOwner.NONE }.map { candidate ->
            FrameProbe(candidate) { false }
        })
        assertEquals(FrameOwner.NONE, owner)
    }
}

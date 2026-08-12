package de.robinthor.digiworldexplorer.capture

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSessionStateTest {
    @After
    fun reset() {
        CaptureSessionState.markCaptureStopped()
    }

    @Test
    fun stoppingAutomationKeepsCaptureRestartable() {
        CaptureSessionState.markCaptureStarted()

        assertFalse(CaptureSessionState.snapshot(automationEnabled = true).canStartAutomation)

        val stopped = CaptureSessionState.snapshot(automationEnabled = false)
        assertTrue(stopped.captureActive)
        assertFalse(stopped.automationActive)
        assertTrue(stopped.canStartAutomation)
    }

    @Test
    fun stoppingCaptureDisablesAutomationStart() {
        CaptureSessionState.markCaptureStarted()
        CaptureSessionState.markCaptureStopped()

        val stopped = CaptureSessionState.snapshot(automationEnabled = false)
        assertFalse(stopped.captureActive)
        assertFalse(stopped.canStartAutomation)
    }
}
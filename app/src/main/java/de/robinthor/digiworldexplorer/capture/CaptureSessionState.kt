package de.robinthor.digiworldexplorer.capture

data class CaptureSessionSnapshot(
    val captureActive: Boolean,
    val automationActive: Boolean,
) {
    val canStartAutomation: Boolean
        get() = captureActive && !automationActive
}

object CaptureSessionState {
    @Volatile
    private var captureActive = false

    fun markCaptureStarted() {
        captureActive = true
    }

    fun markCaptureStopped() {
        captureActive = false
    }

    fun snapshot(automationEnabled: Boolean): CaptureSessionSnapshot = CaptureSessionSnapshot(
        captureActive = captureActive,
        automationActive = captureActive && automationEnabled,
    )
}
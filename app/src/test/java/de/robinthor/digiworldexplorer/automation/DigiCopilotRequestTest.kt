package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigiCopilotRequestTest {
    @Test fun onlyExplicitStartActivatesAndStopAlwaysReleases() {
        DigiCopilotRequest.stop()
        assertFalse(DigiCopilotRequest.active())
        DigiCopilotRequest.start()
        assertTrue(DigiCopilotRequest.active())
        DwsExcursionRequest.start()
        assertTrue(DwsExcursionRequest.active())
        DigiCopilotRequest.stop("test")
        assertFalse(DigiCopilotRequest.active())
        assertFalse(DwsExcursionRequest.active())
    }
}

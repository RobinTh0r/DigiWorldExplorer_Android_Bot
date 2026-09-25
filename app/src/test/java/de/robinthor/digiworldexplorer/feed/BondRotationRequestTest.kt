package de.robinthor.digiworldexplorer.feed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BondRotationRequestTest {
    @Test fun `force request is one shot and can be cancelled`() {
        BondRotationRequest.cancel()
        BondRotationRequest.start()
        assertTrue(BondRotationRequest.active())
        BondRotationRequest.complete()
        assertFalse(BondRotationRequest.active())
        BondRotationRequest.start()
        BondRotationRequest.cancel()
        assertFalse(BondRotationRequest.active())
    }
}

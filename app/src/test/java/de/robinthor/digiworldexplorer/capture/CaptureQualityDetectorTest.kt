package de.robinthor.digiworldexplorer.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureQualityDetectorTest {
    @Test fun uniformGraySurfaceIsRejected() {
        assertEquals(CaptureQuality.UNIFORM_GRAY, detect { _, _ -> rgb(92, 92, 92) }.quality)
    }

    @Test fun blackSurfaceIsRejected() {
        assertEquals(CaptureQuality.BLACK, detect { _, _ -> rgb(2, 2, 2) }.quality)
    }

    @Test fun structuredGameImageIsAccepted() {
        assertEquals(CaptureQuality.VALID, detect { x, y -> if ((x / 80 + y / 80) % 2 == 0) rgb(12, 48, 88) else rgb(170, 90, 35) }.quality)
    }

    @Test fun capturedHudOutsideGameCenterDoesNotHideGraySurface() {
        assertEquals(CaptureQuality.UNIFORM_GRAY, detect { _, y -> if (y < 300) rgb(240, 80, 20) else rgb(110, 110, 110) }.quality)
    }

    private fun detect(pixel: (Int, Int) -> Int) = CaptureQualityDetector.detect(1080, 2400, pixel)
    private fun rgb(r: Int, g: Int, b: Int) = (r shl 16) or (g shl 8) or b
}
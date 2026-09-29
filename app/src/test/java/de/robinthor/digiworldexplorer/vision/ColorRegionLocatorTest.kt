package de.robinthor.digiworldexplorer.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorRegionLocatorTest {
    @Test fun findsScaledButtonCenterInsteadOfAssumingCoordinates() {
        val w = 400; val h = 900
        val pixels = IntArray(w * h)
        for (y in 650 until 720) for (x in 210 until 350) pixels[y * w + x] = 0xff00a8e8.toInt()
        val frame = PixelFrame(w, h) { x, y -> pixels[y * w + x] }
        val regions = ColorRegionLocator.find(frame, GameViewport.fit(w, h), NormalizedRect(.1,.5,.95,.9)) {
            it.blue > 180 && it.green > 120 && it.red < 80
        }.filter { it.width > .20 && it.height > .04 }
        assertEquals(1, regions.size)
        assertTrue(regions.single().center.x in 0.68..0.72)
        assertTrue(regions.single().center.y in 0.74..0.77)
    }
}

package de.robinthor.digiworldexplorer.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelFrameTest {
    @Test fun `viewport retains full adaptive game in tall capture`() {
        assertEquals(GameViewport(0, 0, 1080, 2400), GameViewport.fit(1080, 2400))
    }

    @Test fun `dimensions do not invent a centered 9 by 16 window`() {
        assertEquals(GameViewport(0, 0, 1680, 1920), GameViewport.fit(1680, 1920))
    }

    @Test fun `rgb conversion uses OpenCV hue scale`() {
        val red = Rgb(255, 0, 0).hsv()
        val yellow = Rgb(255, 255, 0).hsv()
        assertEquals(0, red.hue)
        assertEquals(30, yellow.hue)
        assertTrue(yellow.saturation >= 254)
    }
}

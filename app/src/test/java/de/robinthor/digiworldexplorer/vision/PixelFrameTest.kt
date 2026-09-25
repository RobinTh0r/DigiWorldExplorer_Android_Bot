package de.robinthor.digiworldexplorer.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelFrameTest {
    @Test fun `viewport centers a 9 by 16 game in tall capture`() {
        assertEquals(GameViewport(0, 240, 1080, 1920), GameViewport.fit(1080, 2400))
    }

    @Test fun `viewport centers a 9 by 16 game in wide capture`() {
        assertEquals(GameViewport(300, 0, 1080, 1920), GameViewport.fit(1680, 1920))
    }

    @Test fun `rgb conversion uses OpenCV hue scale`() {
        val red = Rgb(255, 0, 0).hsv()
        val yellow = Rgb(255, 255, 0).hsv()
        assertEquals(0, red.hue)
        assertEquals(30, yellow.hue)
        assertTrue(yellow.saturation >= 254)
    }
}

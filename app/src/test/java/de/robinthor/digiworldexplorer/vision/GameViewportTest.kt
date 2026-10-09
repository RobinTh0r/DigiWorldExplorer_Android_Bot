package de.robinthor.digiworldexplorer.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class GameViewportTest {
    @Test fun `tall adaptive phone uses its complete game window`() {
        val viewport = GameViewport.fit(582, 1280)
        assertEquals(GameViewport(0, 0, 582, 1280), viewport)
        assertEquals(1046, viewport.pixel(NormalizedPoint(.823, .818)).second)
    }

    @Test fun `wide dimensions alone do not justify cropping a real game window`() {
        assertEquals(GameViewport(0, 0, 1440, 1920), GameViewport.fit(1440, 1920))
        val frame=PixelFrame(1440,1920) { x,_ -> if(x<180 || x>=1260)0xff000000.toInt() else 0xff335577.toInt() }
        assertEquals(GameViewport(180,0,1080,1920),GameViewport.detect(frame))
    }

    @Test fun `current Galaxy Pixel and OnePlus ratios use adaptive tall layout`() {
        listOf(
            1440 to 3200, // Galaxy S Ultra 20:9
            1080 to 2340, // Galaxy and Pixel 19.5:9
            1344 to 2992, // Pixel Pro-class display
            582 to 1280,  // reported OnePlus capture
        ).forEach { (width, height) ->
            assertEquals("$width x $height", true, GameViewport.fit(width, height).usesTallPhoneLayout)
        }
        assertEquals(false, GameViewport.fit(1080, 1920).usesTallPhoneLayout)
    }
}

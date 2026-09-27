package de.robinthor.digiworldexplorer.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class GameViewportTest {
    @Test fun `tall adaptive phone uses its complete game window`() {
        val viewport = GameViewport.fit(582, 1280)
        assertEquals(GameViewport(0, 0, 582, 1280), viewport)
        assertEquals(1046, viewport.pixel(NormalizedPoint(.823, .818)).second)
    }

    @Test fun `wide capture still removes pillar boxes`() {
        assertEquals(GameViewport(180, 0, 1080, 1920), GameViewport.fit(1440, 1920))
    }
}

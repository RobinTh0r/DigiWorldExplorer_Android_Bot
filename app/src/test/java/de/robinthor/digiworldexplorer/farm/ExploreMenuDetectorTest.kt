package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreMenuDetectorTest {
    @Test fun liveExploreHasNavigableFieldCard() {
        val image = javax.imageio.ImageIO.read(javaClass.getResource("/farm_explore_live.png"))
        val result = ExploreMenuDetector.detect(PixelFrame(image.width, image.height) { x, y -> image.getRGB(x, y) })
        assertNotNull(result.meatFieldTarget)
    }
    @Test fun `dark menu plus anchored cards identifies meat field`() {
        val pixels = IntArray(360 * 640) { 0xff101010.toInt() }
        fun rect(cx: Int, cy: Int, w: Int, h: Int, color: Int) {
            for (y in cy - h / 2 until cy + h / 2) for (x in cx - w / 2 until cx + w / 2)
                pixels[y * 360 + x] = color
        }
        // Dark blue body, magenta World Search anchor, green Meat Field anchor.
        rect(180, 326, 360, 448, 0xff00556e.toInt())
        rect(106, 137, 35, 20, 0xffff00ff.toInt())
        rect(132, 293, 35, 20, 0xff80ff00.toInt())
        val result = ExploreMenuDetector.detect(PixelFrame(360, 640) { x, y -> pixels[y * 360 + x] })
        assertTrue(result.menu)
        assertNotNull(result.worldSearchTarget)
        assertNotNull(result.meatFieldTarget)
    }
}

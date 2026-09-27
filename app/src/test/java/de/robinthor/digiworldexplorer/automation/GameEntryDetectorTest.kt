package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class GameEntryDetectorTest {
    @Test fun `live dark city home is recognized while blocking menus remain excluded`() {
        fun read(name: String): EntryScreen {
            val image = javax.imageio.ImageIO.read(javaClass.getResource("/$name.png"))
            return GameEntryDetector.detect(PixelFrame(image.width, image.height, image::getRGB)).screen
        }
        assertEquals(EntryScreen.HOME, read("home_dark_city"))
        assertEquals(EntryScreen.HOME, read("home_datagrotto_active"))
        assertEquals(EntryScreen.HOME, read("bond_home_food"))
        for (name in listOf("bond_partner_grid", "bond_raise_prompt", "farm_explore_live",
            "dungeon_list_bottom", "dungeon_challenge_de", "idle_rewards_empty")) {
            org.junit.Assert.assertNotEquals(name, EntryScreen.HOME, read(name))
        }
    }
    private val width = 360
    private val height = 640

    @Test fun `pink artwork alone is not a title`() {
        val pixels = IntArray(width * height) { rgb(125, 38, 155) }
        assertEquals(EntryScreen.UNKNOWN, detect(pixels).screen)
    }

    @Test fun `generic cyan button on purple artwork does not authorize login`() {
        val pixels = IntArray(width * height) { rgb(125, 38, 155) }
        fill(pixels, .20, .815, .80, .87, rgb(25, 135, 205))
        fill(pixels, .44, .83, .56, .852, rgb(245, 245, 245))
        assertEquals(EntryScreen.UNKNOWN, detect(pixels).screen)
    }

    @Test fun `blue splash is unknown and cannot authorize a tap`() {
        val pixels = IntArray(width * height) { rgb(15, 95, 180) }
        assertEquals(EntryScreen.UNKNOWN, detect(pixels).screen)
    }

    @Test fun `stable home navigation chrome recognizes changing battle backgrounds`() {
        val pixels = IntArray(width * height) { rgb(145, 105, 75) }
        fill(pixels, .05, .72, .95, .92, rgb(12, 40, 80))
        fill(pixels, .40, .745, .59, .79, rgb(229, 176, 64))
        fill(pixels, .42, .79, .58, .86, rgb(32, 192, 238))
        fill(pixels, .05, .90, .95, .995, rgb(12, 100, 175))
        fill(pixels, .77, .07, .94, .32, rgb(20, 100, 205))
        assertEquals(EntryScreen.HOME, detect(pixels).screen)
    }

    private fun detect(pixels: IntArray) = GameEntryDetector.detect(PixelFrame(width, height) { x, y -> pixels[y * width + x] })
    private fun fill(pixels: IntArray, x0: Double, y0: Double, x1: Double, y1: Double, color: Int) {
        for (y in (height*y0).toInt() until (height*y1).toInt())
            for (x in (width*x0).toInt() until (width*x1).toInt()) pixels[y*width+x] = color
    }
    private fun rgb(r: Int, g: Int, b: Int) = (255 shl 24) or (r shl 16) or (g shl 8) or b
}

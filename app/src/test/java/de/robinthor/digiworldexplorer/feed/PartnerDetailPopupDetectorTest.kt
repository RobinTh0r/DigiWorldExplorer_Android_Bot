package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

class PartnerDetailPopupDetectorTest {
    @Test fun recognizesAccidentalPartnerCardAcrossCaptureScales() {
        val image = ImageIO.read(javaClass.getResource("/oneplus_bond_partner_popup.jpg"))
        for (width in listOf(image.width, image.width * 2)) {
            val height = image.height * width / image.width
            val frame = PixelFrame(width, height) { x, y ->
                image.getRGB(x * image.width / width, y * image.height / height)
            }
            assertTrue("width=$width", PartnerDetailPopupDetector.detect(frame))
        }
    }

    @Test fun partnerPageAndOtherGameWindowsDoNotAuthorizeBack() {
        for (name in listOf("bond_partner_grid.png", "bond_raise_prompt.png",
                "oneplus_cph2611_partner_expanded.jpg", "bond_home_food.png",
                "dungeon_reward_en.png", "farm_seeds_13_1_0.png")) {
            val image = ImageIO.read(javaClass.getResource("/$name"))
            assertFalse(name, PartnerDetailPopupDetector.detect(
                PixelFrame(image.width, image.height, image::getRGB)))
        }
    }
}

package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.PixelFrame
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeIdleRewardDetectorTest {
    private fun frame(name: String): PixelFrame {
        val image: BufferedImage = ImageIO.read(javaClass.classLoader!!.getResource(name))
        return PixelFrame(image.width, image.height) { x, y -> image.getRGB(x, y) }
    }

    @Test fun recognizesConfirmedHomeRewardBox() {
        val result = HomeIdleRewardDetector.detect(frame("home_reward_live_720x1280.png"), homeAlreadyConfirmed = true)
        assertTrue(result.toString(), result.available)
        assertNotNull(result.target)
    }

    @Test fun neverTreatsOtherKnownPagesAsHomeReward() {
        assertFalse(HomeIdleRewardDetector.detect(frame("bond_partner_grid.png")).available)
        assertFalse(HomeIdleRewardDetector.detect(frame("farm_water_16.png")).available)
        assertFalse(HomeIdleRewardDetector.detect(frame("title_ready.png")).available)
    }
}

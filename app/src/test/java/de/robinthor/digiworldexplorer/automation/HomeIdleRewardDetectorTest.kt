package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.PixelFrame
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeIdleRewardDetectorTest {
    @Test fun chestFollowsVerticallyShiftedPhoneViewport() {
        val image = ImageIO.read(javaClass.classLoader!!.getResource("home_reward_live_720x1280.png"))
        val pad = 160
        val frame = PixelFrame(image.width, image.height + 2 * pad) { x, y ->
            if (y in pad until pad + image.height) image.getRGB(x, y - pad)
            else 0xff000000.toInt()
        }
        val reward = HomeIdleRewardDetector.detect(frame, homeAlreadyConfirmed = true)
        assertTrue(reward.toString(), reward.available)
        val target=de.robinthor.digiworldexplorer.vision.GameViewport.detect(frame).pixel(reward.target!!)
        assertEquals(.691 * image.height + pad, target.second.toDouble(), image.height*.025)
    }
    @Test fun chestPositionFollowsCaptureScaleAndEmptyChestIsObserved() {
        val image = ImageIO.read(javaClass.classLoader!!.getResource("home_reward_live_720x1280.png"))
        for (width in listOf(360, 720, 1080)) {
            val height = image.height * width / image.width
            val scaled = PixelFrame(width, height) { x, y ->
                image.getRGB(x * image.width / width, y * image.height / height)
            }
            val reward = HomeIdleRewardDetector.detect(scaled, homeAlreadyConfirmed = true)
            assertTrue("width=$width $reward", reward.available)
            assertEquals(.153, reward.target!!.x, .02)
        }
        val empty = HomeIdleRewardDetector.detect(frame("bond_home_food.png"), homeAlreadyConfirmed = true)
        assertTrue(empty.chestVisible)
        assertFalse(empty.available)
    }
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

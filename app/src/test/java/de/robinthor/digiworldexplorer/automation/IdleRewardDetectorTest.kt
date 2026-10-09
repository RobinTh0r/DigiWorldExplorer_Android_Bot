package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import javax.imageio.ImageIO

class IdleRewardDetectorTest {
    @Test fun rewardTargetsFollowVerticallyShiftedPhoneViewport() {
        for ((name, expected) in listOf("idle_rewards_exhausted.png" to IdleRewardScreen.CLAIM,
                "idle_rewards_empty.png" to IdleRewardScreen.EMPTY,
                "idle_rewards_result.png" to IdleRewardScreen.RESULT)) {
            val image = ImageIO.read(javaClass.getResource("/$name"))
            val pad = 160
            val frame = PixelFrame(image.width, image.height + 2 * pad) { x, y ->
                if (y in pad until pad + image.height) image.getRGB(x, y - pad)
                else 0xff000000.toInt()
            }
            val reading = IdleRewardDetector.read(frame)
            assertEquals(name, expected, reading.screen)
            if (expected == IdleRewardScreen.CLAIM) {
                assertNotNull(reading.claimTarget)
                val target=de.robinthor.digiworldexplorer.vision.GameViewport.detect(frame).pixel(reading.claimTarget!!)
                assertEquals(.740 * image.height + pad, target.second.toDouble(), image.height*.035)
            }
        }
    }
    @Test fun claimAndResultTargetsFollowVisibleDialogsAcrossSizes() {
        for ((name, screen) in listOf("idle_rewards_exhausted.png" to IdleRewardScreen.CLAIM,
                "idle_rewards_empty.png" to IdleRewardScreen.EMPTY,
                "idle_rewards_result.png" to IdleRewardScreen.RESULT)) {
            val image = ImageIO.read(javaClass.getResource("/$name"))
            for (width in listOf(360, 720, 1080)) {
                val height = image.height * width / image.width
                val frame = PixelFrame(width, height) { x, y ->
                    image.getRGB(x * image.width / width, y * image.height / height)
                }
                val reading = IdleRewardDetector.read(frame)
                assertEquals("$name width=$width", screen, reading.screen)
                when (screen) {
                    IdleRewardScreen.CLAIM -> {
                        assertNotNull(reading.claimTarget)
                        assertEquals(.632, reading.claimTarget!!.x, .025)
                        assertNotNull(reading.adTarget)
                    }
                    IdleRewardScreen.EMPTY -> assertNull(reading.claimTarget)
                    IdleRewardScreen.RESULT -> {
                        assertNotNull(reading.closeTarget)
                        assertEquals(.82, reading.closeTarget!!.y, .035)
                    }
                    else -> Unit
                }
            }
        }
    }
    private fun detect(name: String): IdleRewardScreen {
        val img = ImageIO.read(javaClass.getResource("/$name"))
        return IdleRewardDetector.detect(PixelFrame(img.width,img.height) { x,y -> img.getRGB(x,y) })
    }
    @Test fun recognizesClaimResultAndEmptyIndependently() {
        assertEquals(IdleRewardScreen.CLAIM, detect("idle_rewards_exhausted.png"))
        assertEquals(IdleRewardScreen.RESULT, detect("idle_rewards_result.png"))
        assertEquals(IdleRewardScreen.EMPTY, detect("idle_rewards_empty.png"))
    }
    @Test fun unrelatedPagesNeverAuthorizeIdleClaim() {
        for (name in listOf("bond_home_food.png", "bond_partner_grid.png", "bond_raise_prompt.png", "farm_explore_live.png", "farm_seeds_13_1_0.png", "dungeon_reward_en.png"))
            assertEquals(name, IdleRewardScreen.NONE, detect(name))
    }
}

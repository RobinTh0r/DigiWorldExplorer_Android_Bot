package de.robinthor.digiworldexplorer.purchase

import de.robinthor.digiworldexplorer.automation.ObservedScreen
import de.robinthor.digiworldexplorer.automation.PassiveScreenClassifier
import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

class SummonRewardScreenDetectorTest {
    @Test fun `Oppo reward grid blocks another summon despite an enabled buy button`() {
        for (name in listOf("oppo_summon_reward_reveal.jpg", "oppo_summon_reward_grid.jpg")) {
            val image = ImageIO.read(javaClass.getResource("/$name"))
            assertTrue(name, RewardPurchaseDetector.detect(image.width, image.height, image::getRGB).recognized)
            assertTrue(name, SummonRewardScreenDetector.detect(image.width, image.height, image::getRGB))
            assertEquals(name, ObservedScreen.SUMMON,
                PassiveScreenClassifier.detect(PixelFrame(image.width, image.height, image::getRGB)))
        }
    }

    @Test fun `reward guard does not claim other recorded pages`() {
        for (name in listOf("home_dark_city.png", "bond_partner_grid.png", "farm_explore_live.png",
            "dungeon_list_bottom.png", "dungeon_challenge_de.png", "idle_rewards_empty.png")) {
            val image = ImageIO.read(javaClass.getResource("/$name"))
            assertFalse(name, SummonRewardScreenDetector.detect(image.width, image.height, image::getRGB))
        }
    }
}

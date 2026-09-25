package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.imageio.ImageIO

class IdleRewardDetectorTest {
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
        for (name in listOf("bond_home_food.png", "bond_partner_grid.png", "bond_raise_prompt.png", "farm_explore_live.png", "farm_seeds_13_1_0.png"))
            assertEquals(name, IdleRewardScreen.NONE, detect(name))
    }
}

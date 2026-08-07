package de.robinthor.digiworldexplorer.dungeon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class DungeonScreenDetectorTest {
    @Test fun challengeDialogIsLanguageIndependent() {
        listOf("dungeon_challenge_en.png", "dungeon_challenge_de.png").forEach { name ->
            val result = detect(name)
            assertEquals(name, DungeonScreen.CHALLENGE, result.screen)
            assertTrue(result.tapX > 0 && result.tapY > 0)
        }
    }

    @Test fun rewardScreenIsLanguageIndependent() {
        listOf("dungeon_reward_en.png", "dungeon_reward_de.png").forEach { name ->
            assertEquals(name, DungeonScreen.REWARD, detect(name).screen)
        }
    }

    @Test fun ordinaryBlueGameScreenIsNotAReward() {
        assertEquals(DungeonScreen.NONE, detect("samsung_1080x2400.png").screen)
    }

    private fun detect(name: String): DungeonDetection {
        val image: BufferedImage = ImageIO.read(javaClass.classLoader!!.getResourceAsStream(name))
        return DungeonScreenDetector.detect(image.width, image.height) { x, y -> image.getRGB(x, y) }
    }
}
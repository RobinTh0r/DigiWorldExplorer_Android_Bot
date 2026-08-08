package de.robinthor.digiworldexplorer.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class NetworkDefenseScreenDetectorTest {
    @Test fun networkDefenseStartTargetsAttemptButton() {
        val result = detect("network_defense_start.png")
        assertEquals(NetworkDefenseScreen.START, result.screen)
        assertTrue(result.tapX > 0 && result.tapY > 0)
    }

    @Test fun diaboromonBannerTargetsGiveUpButton() {
        val result = detect("network_defense_boss.png")
        assertEquals(NetworkDefenseScreen.DIABOROMON, result.screen)
        assertTrue(result.tapY > 0)
    }

    @Test fun ordinaryDigiWorldIsNotNetworkDefense() {
        assertEquals(NetworkDefenseScreen.NONE, detect("samsung_1080x2400.png").screen)
    }

    @Test fun normalVsDungeonIsNotNetworkDefense() {
        assertEquals(NetworkDefenseScreen.NONE, detect("dungeon_challenge_en.png").screen)
        assertEquals(NetworkDefenseScreen.NONE, detect("dungeon_reward_en.png").screen)
    }

    private fun detect(name: String): NetworkDefenseDetection {
        val image: BufferedImage = ImageIO.read(javaClass.classLoader!!.getResourceAsStream(name))
        return NetworkDefenseScreenDetector.detect(image.width, image.height) { x, y -> image.getRGB(x, y) }
    }
}
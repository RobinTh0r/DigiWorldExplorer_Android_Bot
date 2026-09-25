package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.automation.KnownPageDetector
import de.robinthor.digiworldexplorer.automation.ObservedScreen
import de.robinthor.digiworldexplorer.vision.*
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class DungeonListLiveTest {
    @Test fun networkEntryIsNotTheTeamNotice() {
        val image = ImageIO.read(javaClass.getResource("/dungeon_network_entry.png"))
        val frame = PixelFrame(image.width, image.height, image::getRGB)
        assertEquals("network_challenge", DungeonRotationAnalyzer.panelKind(
            frame, GameViewport.fit(frame.width, frame.height), DungeonKey.NETWORK_DEFENSE))
    }
    @Test fun networkNoticeTakesPriorityOverBackgroundChallenge() {
        val image = ImageIO.read(javaClass.getResource("/dungeon_network_confirm.png"))
        val frame = PixelFrame(image.width, image.height, image::getRGB)
        assertEquals("network_confirm", DungeonRotationAnalyzer.panelKind(
            frame, GameViewport.fit(frame.width, frame.height), DungeonKey.NETWORK_DEFENSE))
    }
    @Test fun scrolledListKeepsDungeonIdentityAndCardOrder() {
        val image = ImageIO.read(javaClass.getResource("/dungeon_list_bottom.png"))
        val frame = PixelFrame(image.width, image.height, image::getRGB)
        val reading = DungeonListDetector.detect(frame)
        assertEquals("$reading", DungeonListPosition.BOTTOM, reading.position)
        assertEquals(listOf(DungeonKey.BAKEMON, DungeonKey.DIGIFACTORY,
            DungeonKey.NETWORK_DEFENSE, DungeonKey.METAL_SEA, DungeonKey.DAILY), reading.cards.map { it.key })
        assertTrue("VS must map to the bottom card, not Metal Sea: $reading",
            reading.cards.last().center.y > .78)
        assertEquals(ObservedScreen.DUNGEON_LIST, KnownPageDetector.detect(frame))
    }
}

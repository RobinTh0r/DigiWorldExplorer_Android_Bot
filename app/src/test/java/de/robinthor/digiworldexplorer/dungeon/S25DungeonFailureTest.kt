package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer
import de.robinthor.digiworldexplorer.vision.*
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class S25DungeonFailureTest {
    @Test fun actualBattleAndSkillFlashCannotBecomeReturnedChallengePanel() {
        for(name in listOf("live_demi_battle_not_panel.png","live_demi_skill_flash_not_panel.jpg")) {
            val img=ImageIO.read(javaClass.getResource("/$name"))
            val f=PixelFrame(img.width,img.height,img::getRGB)
            assertNull(name,DungeonPanelDetector.detect(f,DungeonKey.DEMIDEVIMON,GameViewport.fit(f.width,f.height)))
        }
    }
    @Test fun realNineAndEightTicketCardsAreNotZeroBecauseTheyHaveHoles() {
        val img=ImageIO.read(javaClass.getResource("/live_dungeon_positive_9_8.png"))
        val f=PixelFrame(img.width,img.height,img::getRGB)
        val list=DungeonListDetector.detect(f,GameViewport.fit(f.width,f.height))
        for(key in listOf(DungeonKey.DEMIDEVIMON,DungeonKey.BAKEMON)) {
            assertEquals("$key: $list",CounterAvailability.POSITIVE,list.cards.first {it.key==key}.tickets)
        }
    }
    @Test fun pinkAnnouncementWhitePaperIsNotGrayGrowthGuide() {
        val img=ImageIO.read(javaClass.getResource("/live_news_pink_not_failure.png"))
        assertFalse(StageFailedFrameAnalyzer.detect(img.width,img.height,img::getRGB))
    }
    private fun frame(name:String):PixelFrame {
        val img=ImageIO.read(javaClass.getResource("/$name.jpg"))
        return PixelFrame(img.width,img.height,img::getRGB)
    }
    @Test fun failedForegroundCannotConfirmAWinThroughTheBackgroundPanel() {
        for(name in listOf("s25_dungeon_failed","s25_dungeon_failed_dimmed")) {
            val f=frame(name)
            assertTrue(name,StageFailedFrameAnalyzer.detect(f.width,f.height,f::argbAt))
            assertNull(name,DungeonPanelDetector.detect(f,DungeonKey.DEMIDEVIMON,GameViewport.fit(f.width,f.height)))
        }
    }
    @Test fun returnedAttemptPanelIsPositiveTicketNotFailure() {
        val f=frame("s25_dungeon_returned_panel")
        assertFalse(StageFailedFrameAnalyzer.detect(f.width,f.height,f::argbAt))
        val panel=DungeonPanelDetector.detect(f,DungeonKey.DEMIDEVIMON,GameViewport.fit(f.width,f.height))
        assertEquals("challenge",panel?.kind)
        assertEquals(1,panel?.remaining)
        assertEquals(.689,panel!!.target.x,.025)
        assertEquals(.714,panel.target.y,.025)
    }
}

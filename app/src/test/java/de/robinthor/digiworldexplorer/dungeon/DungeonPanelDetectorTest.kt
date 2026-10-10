package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.PixelFrame
import javax.imageio.ImageIO
import org.junit.Test
import org.junit.Assert.*

class DungeonPanelDetectorTest {
    @Test fun homeStageBannerAndBattleEffectsNeverAuthorizeDungeonModalRecovery() {
        val img=ImageIO.read(javaClass.getResource("/live_home_stage_1220x2712.png"))
        val frame=PixelFrame(img.width,img.height,img::getRGB)
        for(key in DungeonKey.entries)assertNull(key.toString(),DungeonPanelDetector.detect(frame,key))
    }
    @Test fun nativeTallFactoryAdButtonAndZeroFollowMeasuredTitleAndAction() {
        val img=ImageIO.read(javaClass.getResource("/live_dungeon_factory_zero_1220x2712.png"))
        val result=DungeonPanelDetector.detect(PixelFrame(img.width,img.height,img::getRGB),DungeonKey.DIGIFACTORY)
        assertEquals(result.toString(),"ad",result?.kind)
        assertEquals(0,result?.remaining)
        assertEquals(.50,result!!.target.x,.02)
        assertEquals(.683,result.target.y,.025)
    }
    private fun check(file: String, key: DungeonKey, kind: String, count: Int?) {
        val img = ImageIO.read(javaClass.getResource("/$file.png"))
        val result = DungeonPanelDetector.detect(PixelFrame(img.width,img.height,img::getRGB),key)
        assertEquals("$file: $result",kind,result?.kind)
        if(count != null) assertEquals("$file: $result",if(count > 0) 1 else 0,result?.remaining)
    }
    @Test fun readsRealPanels() {
        check("dungeon_normal_two",DungeonKey.DEMIDEVIMON,"challenge",2)
        check("dungeon_ad_two",DungeonKey.DEMIDEVIMON,"ad",2)
        check("dungeon_metal_two",DungeonKey.METAL_SEA,"challenge",2)
        check("dungeon_vs_zero",DungeonKey.DAILY,"destroy",0)
        check("dungeon_network_confirm",DungeonKey.NETWORK_DEFENSE,"network_confirm",null)
        check("dungeon_network_leave",DungeonKey.NETWORK_DEFENSE,"network_leave",null)
        check("dungeon_network_entry",DungeonKey.NETWORK_DEFENSE,"network_matching",2)
    }
    @Test fun networkNoticeTargetsFollowTheirVisibleButtonRows() {
        fun panel(file: String): DungeonPanel {
            val img = ImageIO.read(javaClass.getResource("/$file.png"))
            return requireNotNull(DungeonPanelDetector.detect(PixelFrame(img.width,img.height,img::getRGB),DungeonKey.NETWORK_DEFENSE))
        }
        val confirm = panel("dungeon_network_confirm")
        val leave = panel("dungeon_network_leave")
        assertEquals(.59, confirm.target.y, .04)
        assertEquals(.59, leave.target.y, .04)
        assertTrue(leave.target.x > confirm.target.x)
    }
}

package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.PixelFrame
import javax.imageio.ImageIO
import org.junit.Test
import org.junit.Assert.*

class DungeonPanelDetectorTest {
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
        check("dungeon_network_entry",DungeonKey.NETWORK_DEFENSE,"network_challenge",2)
    }
}

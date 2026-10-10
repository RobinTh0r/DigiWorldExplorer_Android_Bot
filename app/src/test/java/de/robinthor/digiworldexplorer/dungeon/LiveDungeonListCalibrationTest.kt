package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.*
import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO

class LiveDungeonListCalibrationTest {
    @Test fun nativeBottomListAt2424DoesNotShiftMetalSeaOntoNetwork() {
        val img=ImageIO.read(javaClass.getResource("/live_dungeon_list_bottom_1080x2424.png"))
        val frame=PixelFrame(img.width,img.height,img::getRGB)
        val mask=BooleanArray(180*320)
        for(y in 0 until 320)for(x in 0 until 180) {
            val hsv=frame.rgbAt(x*frame.width/180,y*frame.height/320).hsv()
            mask[y*180+x]=hsv.hue in 90..115 && hsv.saturation>=90 && hsv.value>=115
        }
        val parts=ColorComponents.find(mask,180,320).filter {it.width>100 && it.pixels>=75}
        val reading=DungeonListDetector.detect(frame,GameViewport.fit(img.width,img.height))
        assertEquals("$reading components=$parts",DungeonKey.BAKEMON,reading.cards.first().key)
        assertEquals(reading.toString(),listOf(DungeonKey.BAKEMON,DungeonKey.DIGIFACTORY,DungeonKey.NETWORK_DEFENSE,DungeonKey.METAL_SEA,DungeonKey.DAILY),reading.cards.map {it.key})
        val metal=reading.cards.first {it.key==DungeonKey.METAL_SEA}
        assertTrue("$reading components=$parts",metal.center.y in .65.. .75)
    }
    @Test fun recordedListHeadersRemainVisibleAcrossOldLayouts() {
        for(name in listOf("dungeon_list_bottom")) {
            val img=ImageIO.read(javaClass.getResource("/$name.png"))
            assertTrue(name,DungeonListDetector.visibleHeader(PixelFrame(img.width,img.height,img::getRGB),GameViewport.fit(img.width,img.height)))
        }
    }
    @Test fun nativeTopListAt1080HasApocalymonAndFactoryInCorrectOrder() {
        val img=ImageIO.read(javaClass.getResource("/live_dungeon_list_top_1080x2340.png"))
        val frame=PixelFrame(img.width,img.height,img::getRGB)
        val v=GameViewport.fit(img.width,img.height)
        assertTrue(DungeonListDetector.visibleHeader(frame,v))
        val reading=DungeonListDetector.detect(frame,v)
        assertEquals(reading.toString(),DungeonListPosition.TOP,reading.position)
        assertEquals(DungeonKey.APOCALYMON_WALL,reading.cards.first().key)
        assertTrue(reading.cards.any {it.key==DungeonKey.DIGIFACTORY})
    }
    @Test fun dimmedListHeaderBehindRealFactoryModalCannotAuthorizeNavigation() {
        val img=ImageIO.read(javaClass.getResource("/live_dungeon_factory_zero_1220x2712.png"))
        assertFalse(DungeonListDetector.visibleHeader(PixelFrame(img.width,img.height,img::getRGB),GameViewport.fit(img.width,img.height)))
    }
}

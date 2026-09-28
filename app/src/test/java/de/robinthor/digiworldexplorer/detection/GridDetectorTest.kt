package de.robinthor.digiworldexplorer.detection

import org.junit.Assert.*
import org.junit.Test

class GridDetectorTest {
    @Test fun bottomAnchoredFallbackDoesNotClaimOtherGamePages() {
        for (name in listOf("home_dark_city.png", "bond_partner_grid.png", "farm_explore_live.png",
            "dungeon_list_bottom.png", "dungeon_challenge_de.png", "idle_rewards_empty.png",
            "oppo_summon_reward_grid.jpg")) {
            val image = javax.imageio.ImageIO.read(javaClass.getResource("/$name"))
            val pixels = IntArray(image.width * image.height)
            image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
            assertNull(name, GridDetector.detect(image.width, image.height, pixels))
        }
    }
    @Test fun detectsRelativeGridAtArbitraryResolution(){
        val w=1000;val h=1600;val pixels=IntArray(w*h){0xff101820.toInt()}
        val left=120;val top=480;val xStep=140;val yStep=125
        fun paint(x:Int,y:Int){if(x in 0 until w&&y in 0 until h)pixels[y*w+x]=0xffe8f8ff.toInt()}
        for(i in 0..5){val x=left+i*xStep;for(y in (h*.30).toInt() until (h*.70).toInt())paint(x,y)}
        for(i in 0..5){val y=top+i*yStep;for(x in (w*.10).toInt() until (w*.90).toInt())paint(x,y)}
        val d=GridDetector.detect(w,h,pixels)
        assertNotNull(d);d!!
        assertTrue(kotlin.math.abs(left-d.bounds.left)<=3);assertTrue(kotlin.math.abs(top-d.bounds.top)<=3)
        assertTrue(kotlin.math.abs(left+5*xStep-d.bounds.right)<=3);assertTrue(kotlin.math.abs(top+5*yStep-d.bounds.bottom)<=3)
        assertTrue(d.confidence>=.70)
    }
    @Test fun rejectsFlatScreen(){
        assertNull(GridDetector.detect(720,1280,IntArray(720*1280){0xff202830.toInt()}))
    }
}

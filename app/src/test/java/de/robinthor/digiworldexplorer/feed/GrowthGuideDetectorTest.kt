package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class GrowthGuideDetectorTest {
    private fun image(name:String)=ImageIO.read(requireNotNull(javaClass.getResource("/$name")))
    @Test fun bothNewReportedFramesHaveMeasuredSafeOutsideTargets() {
        for(name in listOf("growth_guide_report_native.png","growth_guide_report_zip.jpg")) {
            val img=image(name)
            val reading=requireNotNull(GrowthGuideDetector.observe(PixelFrame(img.width,img.height,img::getRGB))) {name}
            assertTrue(name,reading.dismissTargets.isNotEmpty())
            for(target in reading.dismissTargets) {
                assertTrue(name,target.x<reading.header.left || target.x>reading.header.right)
                // Actual native guide is x103..978 (half scale in the ZIP). Neither
                // target can land on its Fight/Hologram/Skills/Digimon shortcuts.
                assertTrue(name,target.x/img.width<.095 || target.x/img.width>.905)
            }
        }
    }
    @Test fun existingBrightAndDimmedGuidesStillMatch() {
        for(name in listOf("s25_dungeon_failed.jpg","s25_dungeon_failed_dimmed.jpg")) {
            val img=image(name)
            assertNotNull(name,GrowthGuideDetector.observe(PixelFrame(img.width,img.height,img::getRGB)))
        }
    }
    @Test fun announcementCombatNetworkPartnerAndSummonNeverBecomeGuide() {
        for(name in listOf("live_news_pink_not_failure.png","live_demi_battle_not_panel.png",
            "live_demi_skill_flash_not_panel.jpg","network_defense_start.png","network_defense_boss.png",
            "dungeon_network_leave.png","dungeon_network_confirm.png","bond_partner_grid.png",
            "oneplus_summon_purple_result.jpg","dungeon_reward_en.png")) {
            val img=image(name)
            assertNull(name,GrowthGuideDetector.observe(PixelFrame(img.width,img.height,img::getRGB)))
        }
    }
    @Test fun simulatedResolutionAndBlackBordersRetainCorrectTargetOffsets() {
        val source=image("growth_guide_report_native.png")
        for((w,h) in listOf(1080 to 2340,1080 to 2400,720 to 1612,1440 to 3088)) {
            for(border in listOf(0,48)) {
                val img=BufferedImage(w,h,BufferedImage.TYPE_INT_RGB)
                val g=img.createGraphics()
                try {g.drawImage(source,border,border,w-2*border,h-2*border,null)}finally{g.dispose()}
                val reading=requireNotNull(GrowthGuideDetector.observe(PixelFrame(w,h,img::getRGB))) {"$w x $h border=$border"}
                assertTrue(reading.dismissTargets.isNotEmpty())
                for(target in reading.dismissTargets) {
                    assertTrue(target.x>=border && target.x<w-border && target.y>=border && target.y<h-border)
                    assertTrue(target.x<reading.header.left || target.x>reading.header.right)
                }
            }
        }
    }
}

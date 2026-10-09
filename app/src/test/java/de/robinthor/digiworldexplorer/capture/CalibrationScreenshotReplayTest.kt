package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.vision.*
import de.robinthor.digiworldexplorer.feed.PartnerGridDetector
import de.robinthor.digiworldexplorer.dungeon.DungeonKey
import de.robinthor.digiworldexplorer.dungeon.DungeonPanelDetector
import de.robinthor.digiworldexplorer.farm.FarmDialogDetector
import de.robinthor.digiworldexplorer.farm.FarmView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.imageio.ImageIO
import kotlin.math.min

/** Recorded UIs resized uniformly inside simulated display windows. This deliberately
 * does NOT claim to simulate Android font reflow, game UI updates, or physical devices. */
@RunWith(Parameterized::class)
class CalibrationScreenshotReplayTest(private val width:Int,private val height:Int,private val scaledCapture:Boolean) {
    private data class Replay(val frame:PixelFrame,val geometry:FrameGeometry)
    private fun replay(file:String):Replay {
        val source=ImageIO.read(requireNotNull(javaClass.getResource("/$file")))
        val display=PixelRect(0,0,width,height)
        val window=PixelRect(0,36,width,height-96)
        val capture=if(scaledCapture)PixelSize(width/2,height/2)else PixelSize(width,height)
        val p=ProjectionTransform(capture,display)
        val factor=min((width-32).toDouble()/source.width,(window.height-48).toDouble()/source.height)
        val gw=(source.width*factor).toInt();val gh=(source.height*factor).toInt()
        val game=PixelRect((width-gw)/2,window.top+19,(width-gw)/2+gw,window.top+19+gh)
        val raw=PixelFrame(capture.width,capture.height) { x,y ->
            val point=p.toDisplay(x.toDouble(),y.toDouble())
            if(game.contains(point.x,point.y))source.getRGB(((point.x-game.left)/factor).toInt().coerceIn(0,source.width-1),((point.y-game.top)/factor).toInt().coerceIn(0,source.height-1)) else if(window.contains(point.x,point.y))0xff000000.toInt() else 0xff444444.toInt()
        }
        val content=requireNotNull(VisibleGameArea.detect(raw,requireNotNull(p.captureRect(window))))
        val geometry=FrameGeometry(capture,DisplayGeometry(display,window,if(scaledCapture)640 else 320,0),content)
        return Replay(PixelFrame(content.width,content.height){x,y->raw.argbAt(x+content.left,y+content.top)},geometry)
    }
    private fun targetLandsInsideGame(r:Replay,target:NormalizedPoint) {
        val (x,y)=GameViewport.detect(r.frame).pixel(target)
        val point=requireNotNull(r.geometry.map(x.toDouble(),y.toDouble()))
        assertTrue(r.geometry.display.gameWindow.contains(point.x,point.y))
        val roundtrip=r.geometry.projection.toCapture(point.x,point.y)
        assertEquals(x.toDouble(),roundtrip.x-r.geometry.gameInCapture.left,1e-7)
        assertEquals(y.toDouble(),roundtrip.y-r.geometry.gameInCapture.top,1e-7)
    }
    @Test fun partnerExpansionAndAllFifteenSlotsSurviveCaptureWindowOffsets() {
        val collapsed=replay("bond_partner_collapsed_1_5_bluestacks.png")
        val before=PartnerGridDetector.detect(collapsed.frame)
        assertTrue(before.page);assertFalse(before.expanded)
        targetLandsInsideGame(collapsed,requireNotNull(before.expandTarget))
        val expanded=replay("bond_partner_1_5_bluestacks.png")
        val after=PartnerGridDetector.detect(expanded.frame)
        assertTrue(after.expanded);assertEquals(15,after.cells.size);assertEquals(1,after.selected)
        after.cells.forEach { targetLandsInsideGame(expanded,it) }
    }
    @Test fun networkAdAndMatchingTargetsRemainVisuallyRecognizable() {
        for((file,kind) in listOf("dungeon_network_one_ad_tall.png" to "ad","dungeon_network_entry.png" to "network_matching")) {
            val r=replay(file)
            val panel=DungeonPanelDetector.detect(r.frame,DungeonKey.NETWORK_DEFENSE)
            assertEquals(file,kind,panel?.kind)
            targetLandsInsideGame(r,requireNotNull(panel).target)
        }
    }
    @Test fun meatFieldSeedPickerRetainsThreeSlotsAndSelectionTarget() {
        val r=replay("oneplus_farm_seed_dialog.jpg")
        val dialog=FarmDialogDetector.detect(r.frame,6,trustedFarmFlow=true)
        assertEquals(FarmView.SEEDS,dialog.view);assertEquals(3,dialog.slots.size)
        targetLandsInsideGame(r,requireNotNull(dialog.selectButton))
        dialog.slots.forEach { targetLandsInsideGame(r,it) }
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}x{1}, scaledCapture={2}")
        fun cases():List<Array<Any>> = listOf(1080 to 2340,1080 to 2400,720 to 1612,1440 to 3200,1080 to 1920,1536 to 2048,2560 to 1600,480 to 800).flatMap { (w,h)->listOf(false,true).map { arrayOf<Any>(w,h,it) } }
    }
}

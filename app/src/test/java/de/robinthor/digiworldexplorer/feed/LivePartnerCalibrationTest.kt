package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*
import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO

/** Native emulator screenshots after real client restarts, NOT resized old fixtures. */
class LivePartnerCalibrationTest {
    @Test fun bubbleFollowsTheMovingPartnerInsteadOfUsingASingleCenterTap() {
        for((name,x) in listOf("left" to .398,"right" to .541)) {
            val image=ImageIO.read(javaClass.getResource("/live_bubble_moving_$name.jpg"))
            val bubble=BondBubbleDetector.detect(PixelFrame(image.width,image.height,image::getRGB))
            assertNotNull(name,bubble)
            assertEquals(name,x,bubble!!.x,.018)
            assertEquals(name,.396,bubble.y,.015)
        }
    }
    @Test fun cyanBattleProjectileMustNotWinOverActualFoodBubble() {
        for((name,x) in listOf("before" to .535,"after" to .520)) {
            val image=ImageIO.read(javaClass.getResource("/live_bubble_projectile_$name.jpg"))
            val frame=PixelFrame(image.width,image.height,image::getRGB)
            val bubble=BondBubbleDetector.detect(frame)
            assertNotNull(name,bubble)
            assertEquals(name,x,bubble!!.x,.025)
            assertEquals(name,.397,bubble.y,.018)
        }
    }
    @Test fun nativeExpandedRosemonHasObservedFifteenCellsAndActualActiveMarker() {
        val image=ImageIO.read(javaClass.getResource("/live_partner_expanded_1220x2712.png"))
        val frame=PixelFrame(image.width,image.height,image::getRGB)
        val grid=PartnerGridDetector.detect(frame)
        assertNull("A minus or artwork must not become a plus: $grid",PartnerVisualLocator.expandTarget(frame,GameViewport.detect(frame)))
        assertTrue(grid.toString(),grid.expanded)
        assertEquals(15,grid.cells.size)
        assertEquals(4,grid.selected)
        assertEquals(4,grid.raised)
    }
    @Test fun nativeReflowCollapsedPagesNeverAuthorizeFifteenCellSelection() {
        for (size in listOf("1080x2340", "1080x2424", "1440x3168", "1220x2712")) {
            val image = ImageIO.read(javaClass.getResource("/live_partner_$size.png"))
            val frame = PixelFrame(image.width, image.height, image::getRGB)
            val grid = PartnerGridDetector.detect(frame)
            assertTrue("$size $grid", grid.page)
            assertTrue("$size $grid", grid.digimonSection)
            assertFalse("$size $grid", grid.expanded)
            assertTrue("$size $grid", grid.cells.isEmpty())
            assertNotNull("$size $grid", grid.expandTarget)
            assertEquals("$size", .89, grid.expandTarget!!.x, .02)
            assertEquals("$size", .82, grid.expandTarget.y, .02)
            assertEquals("$size", BondStep.EXPAND, BondRotation().tick(false, grid, false, 0)?.step)
            assertFalse("Partner sheet is not Home: $size",
                de.robinthor.digiworldexplorer.automation.HomeScreenDetector.detect(frame.width,frame.height,frame::argbAt))
        }
    }
}

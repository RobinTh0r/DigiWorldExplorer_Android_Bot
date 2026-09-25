package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO

class PartnerGridDetectorTest {
    @Test fun maxLevelUsesRightStartButton() {
        val grid = read("bond_partner_max.png")
        assertEquals(14, grid.raised)
        assertEquals(0, grid.selected)
        assertTrue(grid.canRaise)
        assertEquals(.632, grid.raiseTarget!!.x, .001)
    }
    @Test fun foodBubbleHasWhitePanelAndCyanOutline() {
        val img = ImageIO.read(javaClass.getResource("/bond_home_food.png"))
        val bubble = BondBubbleDetector.detect(PixelFrame(img.width,img.height) { x,y -> img.getRGB(x,y) })
        assertNotNull(bubble)
        assertEquals(.512, bubble!!.x, .02)
        assertEquals(.395, bubble.y, .02)
    }
    private fun read(name: String): PartnerGrid {
        val img = ImageIO.read(javaClass.getResource("/$name"))
        return PartnerGridDetector.detect(PixelFrame(img.width, img.height) { x, y -> img.getRGB(x,y) })
    }
    @Test fun recognizesSelectionSeparatelyFromRaisedPartner() {
        val grid = read("bond_partner_grid.png")
        assertTrue(grid.expanded)
        assertEquals(15, grid.cells.size)
        assertEquals(1, grid.raised)
        assertEquals(1, grid.selected)
        val selected = read("bond_partner_selected.png")
        assertEquals(1, selected.raised)
        assertEquals(2, selected.selected)
        assertTrue(selected.canRaise)
        assertTrue(read("bond_raise_prompt.png").confirmation)
    }
    @Test fun homeAndExploreCannotAuthorizePartnerSelection() {
        assertFalse(read("bond_home_food.png").page)
        assertFalse(read("farm_explore_live.png").page)
    }
}

package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO

class PartnerGridDetectorTest {
    @Test fun expansionRetryNeedsFreshVisiblePlusAndIsBounded() {
        val tour = BondRotation()
        val collapsed = read("bond_partner_collapsed.png")
        assertEquals(BondStep.EXPAND, tour.tick(false, collapsed, false, 0)?.step)
        assertNull(tour.tick(false, collapsed.copy(expandTarget = null), false, 2_000))
        assertEquals(BondStep.EXPAND, tour.tick(false, collapsed, false, 2_001)?.step)
        assertEquals(BondStep.EXPAND, tour.tick(false, collapsed, false, 4_001)?.step)
        assertNull(tour.tick(false, collapsed, false, 6_001))
    }
    @Test fun expandRequiresVisiblePlusAndFollowsItsPosition() {
        val img = ImageIO.read(javaClass.getResource("/bond_partner_collapsed.png"))
        for ((dx, dy) in listOf(0 to 0, -4 to -5, 4 to 5)) {
            val frame = PixelFrame(img.width, img.height) { x, y ->
                img.getRGB((x - dx).coerceIn(0, img.width - 1), (y - dy).coerceIn(0, img.height - 1))
            }
            val target = PartnerVisualLocator.expandTarget(frame,
                de.robinthor.digiworldexplorer.vision.GameViewport.fit(img.width, img.height))
            assertNotNull("offset=$dx,$dy", target)
            assertEquals(.823 + dx.toDouble() / img.width, target!!.x, .006)
            assertEquals(.818 + dy.toDouble() / img.height, target.y, .006)
        }
        assertNotNull(read("bond_partner_collapsed.png").expandTarget)
        assertNull(read("bond_partner_grid.png").expandTarget)
        assertNull(read("oneplus_cph2611_partner_expanded.jpg").expandTarget)
    }
    @Test fun confirmationTargetFollowsDialogInsteadOfFixedScreenPoint() {
        val img = ImageIO.read(javaClass.getResource("/bond_raise_prompt.png"))
        for ((dx, dy) in listOf(0 to 0, -18 to -35, 18 to 35)) {
            val frame = PixelFrame(img.width, img.height) { x, y ->
                img.getRGB((x - dx).coerceIn(0, img.width - 1), (y - dy).coerceIn(0, img.height - 1))
            }
            val grid = PartnerGridDetector.detect(frame)
            assertTrue("offset=$dx,$dy", grid.confirmation)
            assertEquals(.634 + dx.toDouble() / img.width, grid.confirmationTarget!!.x, .012)
            assertEquals(.59 + dy.toDouble() / img.height, grid.confirmationTarget.y, .012)
        }
        for (name in listOf("bond_partner_grid.png", "bond_partner_max.png", "bond_home_food.png", "farm_explore_live.png", "dungeon_challenge_en.png")) {
            assertFalse(name, read(name).confirmation)
            assertNull(name, read(name).confirmationTarget)
        }
    }
    @Test fun rosterCoordinatesFollowVisibleLayoutOffsets() {
        val img = ImageIO.read(javaClass.getResource("/oneplus_cph2611_partner_expanded.jpg"))
        for ((dx, dy) in listOf(-3 to -6, 3 to 6)) {
            val frame = PixelFrame(img.width, img.height) { x, y ->
                img.getRGB((x - dx).coerceIn(0, img.width - 1), (y - dy).coerceIn(0, img.height - 1))
            }
            val grid = PartnerGridDetector.detect(frame)
            assertTrue("offset=$dx,$dy", grid.expanded)
            assertEquals(5, grid.raised)
            assertEquals(5, grid.selected)
            assertEquals(.16389 + dx.toDouble() / img.width, grid.cells[0].x, .007)
            assertEquals(.6612 + dy.toDouble() / img.height, grid.cells[0].y, .007)
        }
    }
    @Test fun observedRosterSurvivesCaptureScaling() {
        val img = ImageIO.read(javaClass.getResource("/oneplus_cph2611_partner_expanded.jpg"))
        for (width in listOf(360, 720, 1080, 1440)) {
            val height = img.height * width / img.width
            val frame = PixelFrame(width, height) { x, y -> img.getRGB(x * img.width / width, y * img.height / height) }
            val grid = PartnerGridDetector.detect(frame)
            assertTrue("width=$width", grid.expanded)
            assertEquals("width=$width", 5, grid.raised)
            assertEquals("width=$width", 5, grid.selected)
            assertEquals("width=$width", 15, grid.cells.size)
        }
    }
    @Test fun recognizesAlreadyExpandedCPH2611WithoutRetappingPartnerTab() {
        val img = ImageIO.read(javaClass.getResource("/oneplus_cph2611_partner_expanded.jpg"))
        val frame = PixelFrame(img.width, img.height, img::getRGB)
        val v = de.robinthor.digiworldexplorer.vision.GameViewport.fit(img.width, img.height)
        assertEquals(15, PartnerVisualLocator.detect(frame, v)!!.cells.size)
        val grid = read("oneplus_cph2611_partner_expanded.jpg")
        assertTrue(grid.page)
        assertTrue(grid.expanded)
        assertEquals(15, grid.cells.size)
        assertEquals(5, grid.raised)
        assertEquals(grid.toString(), 5, grid.selected)
        val command = BondRotation().tick(false, grid, false, 0)
        assertEquals(BondStep.SELECT, command?.step)
        assertEquals(6, command?.cell)
        val tour = BondRotation()
        tour.tick(true, PartnerGrid(), false, 0)
        tour.tick(false, PartnerGrid(digimonSection = true,
            partnerTabTarget = de.robinthor.digiworldexplorer.vision.NormalizedPoint(.115, .875)), false, 1)
        val afterSwitchingTab = tour.tick(false, grid, false, 2)
        assertEquals("An already expanded grid must never receive a collapse tap", BondStep.SELECT, afterSwitchingTab?.step)
        assertEquals(6, afterSwitchingTab?.cell)
    }
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
    @Test fun recognizesExpandedOnePlusRoster() {
        val grid = read("oneplus_partner_grid_expanded.jpg")
        assertTrue(grid.page)
        assertTrue(grid.digimonSection)
        assertNotNull(grid.partnerTabTarget)
        assertTrue(grid.expanded)
        assertEquals(15, grid.cells.size)
        assertEquals("The green active-partner check is on the first row, fourth cell", 3, grid.raised)
    }

    @Test fun `only Digimon section evidence authorizes Partner tab recovery`() {
        val partner = read("bond_partner_collapsed.png")
        assertTrue(partner.digimonSection)
        assertNotNull(partner.partnerTabTarget)
        assertFalse(read("bond_home_food.png").digimonSection)
        assertFalse(read("farm_explore_live.png").digimonSection)
        assertFalse(read("dungeon_challenge_en.png").digimonSection)
    }
}

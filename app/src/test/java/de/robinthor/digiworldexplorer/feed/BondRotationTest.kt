package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import org.junit.Assert.*
import org.junit.Test

class BondRotationTest {
    @Test fun restartOnOpenPartnerPageResumesWithoutBlindHomeOpenTap() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        val command = tour.tick(false, PartnerGrid(true, true, cells, raised = 4), false, 0)
        assertEquals(BondStep.SELECT, command?.step)
        assertEquals(5, command?.cell)
        assertEquals(4, tour.original)
    }

    @Test fun restartOnCollapsedPartnerPageRequestsExpand() {
        val tour = BondRotation()
        assertEquals(BondStep.EXPAND, tour.tick(false, PartnerGrid(page = true), false, 0)?.step)
    }

    @Test fun blindFallbackNeverAdvancesCollection() {
        val tour = BondRotation()
        val grid = PartnerGrid(true, true, List(15) { NormalizedPoint(.2, .6) }, raised = 0, selected = 0)
        tour.tick(true, PartnerGrid(), false, 0)
        tour.tick(false, grid, false, 1)
        tour.tick(false, grid.copy(selected = 1, canRaise = true), false, 2)
        tour.tick(false, PartnerGrid(confirmation = true), false, 3)
        tour.tick(false, grid.copy(raised = 1), false, 4)
        tour.tick(true, PartnerGrid(), false, 5)
        assertNull(tour.tick(true, PartnerGrid(), false, 5_999))
        assertNull(tour.tick(true, PartnerGrid(), false, 8_000, bubbleVisible = true))
        assertNull(tour.tick(false, PartnerGrid(), false, 8_100))
        assertNull(tour.tick(true, PartnerGrid(), false, 9_000))
        assertEquals(BondStep.COLLECT, tour.step)
    }

    @Test fun droppedOpenRetriesOnlyOnHomeAndKeepsOriginalDeadline() {
        val tour = BondRotation()
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, 0)?.step)
        assertNull(tour.tick(true, PartnerGrid(), false, 3_999))
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, 4_000)?.step)
        assertNull(tour.tick(false, PartnerGrid(confirmation = true), false, 8_000))
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, 9_000)?.step)
        assertNull(tour.tick(true, PartnerGrid(), false, 13_000))
        assertNull(tour.tick(true, PartnerGrid(), false, 25_000))
        assertEquals(BondStep.PARK, tour.step)
    }

    @Test fun openRetryContinuesOnlyAfterRecognizedPartnerGrid() {
        val tour = BondRotation()
        tour.tick(true, PartnerGrid(), false, 0)
        tour.tick(true, PartnerGrid(), false, 4_000)
        val cells = List(15) { NormalizedPoint(.2, .6) }
        val select = tour.tick(false, PartnerGrid(true, true, cells, raised = 14, selected = 14), false, 5_000)
        assertEquals(BondStep.SELECT, select?.step)
        assertEquals(0, select?.cell)
        assertEquals(14, tour.original)
    }

    @Test fun yellowSelectionAloneCannotPretendToBeTheActivePartner() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        tour.tick(true, PartnerGrid(), false, 0)
        val select = tour.tick(false,
            PartnerGrid(true, true, cells, raised = null, selected = 10, canRaise = false), false, 1)
        assertNull(select)
        assertNull(tour.original)
    }

    @Test fun allFifteenPartnersEndWithOriginalGreenCheck() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        var now = 0L
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, now++)?.step)
        var active = 1
        val visited = mutableListOf<Int>()
        repeat(15) {
            val grid = PartnerGrid(true, true, cells, raised = active, selected = active)
            val select = tour.tick(false, grid, false, now++)!!
            assertEquals(BondStep.SELECT, select.step)
            val target = select.cell!!
            visited += target
            assertEquals(BondStep.RAISE, tour.tick(false, grid.copy(selected = target, canRaise = true), false, now++)?.step)
            assertEquals(BondStep.CONFIRM, tour.tick(false, PartnerGrid(confirmation = true), false, now++)?.step)
            active = target
            assertEquals(BondStep.HOME, tour.tick(false, grid.copy(raised = active), false, now++)?.step)
            tour.tick(true, PartnerGrid(), false, now)
            now += 5_001
            tour.tick(true, PartnerGrid(), false, now++, bubbleVisible = false, bubbleCollected = true)
        }
        assertEquals(15, visited.toSet().size)
        assertEquals(1, active)
        assertEquals(1, tour.original)
        assertEquals(BondStep.REST, tour.step)
        assertNull(tour.tick(true, PartnerGrid(), false, now + 1, bubbleVisible = true, cycleReady = false))
        assertNull(tour.tick(true, PartnerGrid(), false, now + 600_000, cycleReady = false))
        assertEquals(BondStep.REST, tour.step)
        assertNull(tour.tick(false, PartnerGrid(), false, now + 600_001, bubbleVisible = true, cycleReady = true))
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, now + 600_002, bubbleVisible = true, cycleReady = true)?.step)
    }

    @Test fun intermittentBubbleCannotBeSkippedWithoutConfirmedTap() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        tour.tick(true, PartnerGrid(), false, 0)
        val grid = PartnerGrid(true, true, cells, raised = 0, selected = 0)
        tour.tick(false, grid, false, 1)
        tour.tick(false, grid.copy(selected = 1, canRaise = true), false, 2)
        tour.tick(false, PartnerGrid(confirmation = true), false, 3)
        tour.tick(false, grid.copy(raised = 1), false, 4)
        tour.tick(true, PartnerGrid(), false, 5)
        assertNull(tour.tick(true, PartnerGrid(), false, 9_000, bubbleVisible = false))
        assertNull(tour.tick(true, PartnerGrid(), false, 12_000, bubbleVisible = true))
        assertNull(tour.tick(true, PartnerGrid(), false, 20_000, bubbleVisible = false))
        assertNull(tour.tick(true, PartnerGrid(), false, 21_000,
            bubbleVisible = false, bubbleCollected = false))
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, 31_000,
            bubbleVisible = false, bubbleCollected = true)?.step)
    }

    @Test fun confirmedBubbleTapAdvancesEvenWhileCollectionAnimationHidesHome() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        tour.tick(true, PartnerGrid(), false, 0)
        val grid = PartnerGrid(true, true, cells, raised = 0, selected = 0)
        tour.tick(false, grid, false, 1)
        tour.tick(false, grid.copy(selected = 1, canRaise = true), false, 2)
        tour.tick(false, PartnerGrid(confirmation = true), false, 3)
        tour.tick(false, grid.copy(raised = 1), false, 4)
        tour.tick(true, PartnerGrid(), false, 5)
        assertEquals(BondStep.OPEN, tour.tick(
            home = false,
            grid = PartnerGrid(),
            feedBusy = false,
            now = 1_006,
            bubbleCollected = true,
        )?.step)
    }

    @Test fun transientWrongRaisedCellWhileOpeningDoesNotParkTour() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        val first = PartnerGrid(true, true, cells, raised = 0, selected = 0)
        tour.tick(true, PartnerGrid(), false, 0)
        tour.tick(false, first, false, 1)
        tour.tick(false, first.copy(selected = 1, canRaise = true), false, 2)
        tour.tick(false, PartnerGrid(confirmation = true), false, 3)
        tour.tick(false, first.copy(raised = 1), false, 4)
        tour.tick(true, PartnerGrid(), false, 5)
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, 31_000)?.step)

        assertNull(tour.tick(false, first.copy(raised = 0), false, 31_100))
        assertEquals(BondStep.OPEN, tour.step)
        assertEquals(BondStep.SELECT, tour.tick(false, first.copy(raised = 1), false, 31_200)?.step)
    }

    @Test fun foreignPromptNeverStartsTourAndFailedSelectionDoesNotConfirm() {
        val tour = BondRotation()
        assertNull(tour.tick(false, PartnerGrid(confirmation = true), false, 0))
        tour.tick(true, PartnerGrid(), false, 1)
        tour.tick(false, PartnerGrid(confirmation = true), false, 25_001)
        assertEquals(BondStep.PARK, tour.step)
    }

    @Test fun `active marker recovers when fast phone skips confirmation capture`() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        val first = PartnerGrid(true, true, cells, raised = 0, selected = 0)
        tour.tick(true, PartnerGrid(), false, 0)
        tour.tick(false, first, false, 1)
        tour.tick(false, first.copy(selected = 1, canRaise = true), false, 2)
        assertEquals(BondStep.HOME, tour.tick(false, first.copy(raised = 1, selected = 1, canRaise = false), false, 3)?.step)
    }

    @Test fun `selection and confirmation taps retry only on their proven screens`() {
        val tour = BondRotation()
        val cells = List(15) { NormalizedPoint(.2, .6) }
        val grid = PartnerGrid(true, true, cells, raised = 0, selected = 0)
        tour.tick(true, PartnerGrid(), false, 0)
        assertEquals(BondStep.SELECT, tour.tick(false, grid, false, 1)?.step)
        assertNull(tour.tick(false, grid, false, 2_000))
        assertEquals(BondStep.SELECT, tour.tick(false, grid, false, 2_001)?.step)
        assertEquals(BondStep.RAISE, tour.tick(false, grid.copy(selected = 1, canRaise = true), false, 2_002)?.step)
        assertEquals(BondStep.CONFIRM, tour.tick(false, PartnerGrid(confirmation = true), false, 2_003)?.step)
        assertNull(tour.tick(false, PartnerGrid(confirmation = true), false, 3_502))
        assertEquals(BondStep.CONFIRM, tour.tick(false, PartnerGrid(confirmation = true), false, 3_503)?.step)
    }

    @Test fun `wrong Digimon subtab is corrected before roster expansion`() {
        val tour = BondRotation()
        val partnerTarget = NormalizedPoint(.115, .875)
        assertEquals(BondStep.OPEN, tour.tick(true, PartnerGrid(), false, 0)?.step)
        val buddyTab = PartnerGrid(digimonSection = true, partnerTabTarget = partnerTarget)
        assertEquals(BondStep.PARTNER_TAB, tour.tick(false, buddyTab, false, 1)?.step)
        assertNull(tour.tick(false, buddyTab, false, 1_500))
        assertEquals(BondStep.PARTNER_TAB, tour.tick(false, buddyTab, false, 1_501)?.step)
        assertEquals(BondStep.EXPAND, tour.tick(false, PartnerGrid(page = true), false, 1_502)?.step)
    }

    @Test fun `unknown screen never authorizes Partner tab tap`() {
        val tour = BondRotation()
        tour.tick(true, PartnerGrid(), false, 0)
        assertNull(tour.tick(false, PartnerGrid(), false, 5_000))
        assertEquals(BondStep.OPEN, tour.step)
    }
}

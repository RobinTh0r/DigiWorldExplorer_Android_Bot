package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import org.junit.Assert.*
import org.junit.Test

class BondRotationTest {
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
            now += 15_001
            tour.tick(true, PartnerGrid(), false, now++, bubbleVisible = true)
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

    @Test fun foreignPromptNeverStartsTourAndFailedSelectionDoesNotConfirm() {
        val tour = BondRotation()
        assertNull(tour.tick(false, PartnerGrid(confirmation = true), false, 0))
        tour.tick(true, PartnerGrid(), false, 1)
        tour.tick(false, PartnerGrid(confirmation = true), false, 25_001)
        assertEquals(BondStep.PARK, tour.step)
    }
}

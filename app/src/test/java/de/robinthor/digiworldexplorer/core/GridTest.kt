package de.robinthor.digiworldexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GridTest {
    @Test
    fun cellCentersAreRelativeToDetectedBoard() {
        val board = BoardBounds(left = 100, top = 200, right = 600, bottom = 700)

        assertEquals(ScreenPoint(150, 250), board.cellCenter(Cell(0, 0)))
        assertEquals(ScreenPoint(550, 650), board.cellCenter(Cell(4, 4)))
    }
}

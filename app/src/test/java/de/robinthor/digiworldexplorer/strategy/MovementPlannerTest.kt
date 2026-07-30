package de.robinthor.digiworldexplorer.strategy

import de.robinthor.digiworldexplorer.detection.Cell
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

class MovementPlannerTest {
 @Test fun prefersRightOnClearBoard(){assertEquals(Cell(2,3),MovementPlanner.choose(Cell(2,2),emptySet(),emptyList())?.target)}
 @Test fun blockedRightUsesNextRow(){assertEquals(Cell(3,2),MovementPlanner.choose(Cell(2,2),setOf(Cell(2,3)),emptyList())?.target)}
 @Test fun avoidsImmediateReturn(){assertNotEquals(Cell(2,1),MovementPlanner.choose(Cell(2,2),setOf(Cell(2,3)),listOf(Cell(2,1)))?.target)}
 @Test fun breaksHorizontalOscillation(){val h=listOf(Cell(4,0),Cell(4,1),Cell(4,0),Cell(4,1));assertEquals(Cell(3,1),MovementPlanner.choose(Cell(4,1),setOf(Cell(4,2)),h)?.target)}
}
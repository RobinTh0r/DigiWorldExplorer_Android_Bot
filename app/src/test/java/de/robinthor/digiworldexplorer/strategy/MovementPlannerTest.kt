package de.robinthor.digiworldexplorer.strategy
import de.robinthor.digiworldexplorer.detection.*
import org.junit.Assert.*
import org.junit.Test
class MovementPlannerTest{
 private fun board(obstacles:Set<Cell> = emptySet(),items:Set<Cell> = emptySet())=(0..4).flatMap{r->(0..4).map{c->Cell(r,c)}}.associateWith{c->CellScores(0.0,if(c in items).1 else 0.0,0.0,0.0,if(c in items).1 else 0.0,if(c in obstacles).5 else 0.0,0.0)}
 @Test fun prefersRight(){assertEquals(Cell(2,3),MovementPlanner.choose(Cell(2,2),board(),emptyList())?.target)}
 @Test fun routesToItem(){val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(1,2))),emptyList());assertEquals(Cell(1,2),a?.target);assertEquals(ActionKind.MOVE,a?.kind)}
 @Test fun attacksBlockingPyramidOnItemRoute(){val a=MovementPlanner.choose(Cell(2,1),board(setOf(Cell(2,2)),setOf(Cell(2,3))),emptyList());assertEquals(Cell(2,2),a?.target);assertEquals(ActionKind.ATTACK,a?.kind)}
 @Test fun blockedRightAttacks(){assertEquals(ActionKind.ATTACK,MovementPlanner.choose(Cell(2,2),board(setOf(Cell(2,3))),emptyList())?.kind)}
 @Test fun breaksOscillationAtRightEdge(){val h=listOf(Cell(4,3),Cell(4,4),Cell(4,3),Cell(4,4));assertEquals(Direction.UP,MovementPlanner.choose(Cell(4,4),board(),h)?.direction)}
 @Test fun dashOnlyWhenAvailable(){val b=board(setOf(Cell(2,2),Cell(2,3)));assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),b,emptyList(),true)?.kind);assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),b,emptyList(),false)?.kind)}
}
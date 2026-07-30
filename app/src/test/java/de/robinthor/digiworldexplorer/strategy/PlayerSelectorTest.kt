package de.robinthor.digiworldexplorer.strategy
import de.robinthor.digiworldexplorer.detection.*
import org.junit.Assert.assertEquals
import org.junit.Test
class PlayerSelectorTest{
 private fun s(player:Double,item:Double=0.0)=CellScores(player,0.0,item,0.0,item,0.0,0.0)
 @Test fun rejectsRememberedPurpleItem(){val real=Cell(3,1);val purple=Cell(4,2);val cells=mapOf(real to s(.12),purple to s(.22));assertEquals(real,PlayerSelector.select(cells,real,null,setOf(purple))?.key)}
 @Test fun rejectsImpossibleDiagonalJump(){val real=Cell(3,1);val fake=Cell(4,2);assertEquals(real,PlayerSelector.select(mapOf(real to s(.11),fake to s(.23)),real,null,emptySet())?.key)}
 @Test fun acceptsExpectedMove(){val target=Cell(3,2);assertEquals(target,PlayerSelector.select(mapOf(target to s(.10)),Cell(3,1),target,emptySet())?.key)}
}
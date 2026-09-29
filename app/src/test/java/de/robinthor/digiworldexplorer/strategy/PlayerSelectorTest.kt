package de.robinthor.digiworldexplorer.strategy
import de.robinthor.digiworldexplorer.detection.*
import org.junit.Assert.assertEquals
import org.junit.Test
class PlayerSelectorTest{
 private fun s(player:Double,item:Double=0.0)=CellScores(player,0.0,item,0.0,item,0.0,0.0)
 @Test fun rejectsRememberedPurpleItem(){val real=Cell(3,1);val purple=Cell(4,2);val cells=mapOf(real to s(.12),purple to s(.22));assertEquals(real,PlayerSelector.select(cells,real,null,setOf(purple))?.key)}
 @Test fun rejectsImpossibleDiagonalJump(){val real=Cell(3,1);val fake=Cell(4,2);assertEquals(real,PlayerSelector.select(mapOf(real to s(.11),fake to s(.23)),real,null,emptySet())?.key)}
 @Test fun acceptsExpectedMove(){val target=Cell(3,2);assertEquals(target,PlayerSelector.select(mapOf(target to s(.10)),Cell(3,1),target,emptySet())?.key)}
 @Test fun largeSpriteDoesNotJumpToStrongerNeighbourWithoutExpectedMove(){
  val proven=Cell(3,2);val spill=Cell(3,3)
  assertEquals(proven,PlayerSelector.select(mapOf(proven to s(.14),spill to s(.20)),proven,null,emptySet())?.key)
 }

 /**
  * Regression: das eigene Sprite faerbt die Zelle als Item ein, wodurch die Spielerzelle in
  * recentItems landete und der Selektor sie dauerhaft verwarf - die Figur galt fuer immer als
  * "unsicher". Die zuletzt bestaetigte Zelle muss trotz Item-Sperre waehlbar bleiben.
  */
 @Test fun keepsTrackingWhenOwnCellLooksLikeItem(){
  val player=Cell(4,2)
  assertEquals(player,PlayerSelector.select(mapOf(player to s(.16,item=.10)),player,Cell(4,3),setOf(player))?.key)
 }
 @Test fun acceptsNewGenericPartnerEvenWhenItsColoursAlsoLookLikeAnItem(){
  val evolved=Cell(2,1)
  assertEquals(evolved,PlayerSelector.select(mapOf(evolved to s(.14,item=.11)),null,null,emptySet())?.key)
 }
}

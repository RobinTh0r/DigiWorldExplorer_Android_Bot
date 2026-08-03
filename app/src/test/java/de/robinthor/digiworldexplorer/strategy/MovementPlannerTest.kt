package de.robinthor.digiworldexplorer.strategy
import de.robinthor.digiworldexplorer.detection.*
import org.junit.Assert.*
import org.junit.Test
class MovementPlannerTest{
 private fun board(obstacles:Set<Cell> = emptySet(),items:Set<Cell> = emptySet())=(0..4).flatMap{r->(0..4).map{c->Cell(r,c)}}.associateWith{c->CellScores(0.0,if(c in items).1 else 0.0,0.0,0.0,if(c in items).1 else 0.0,if(c in obstacles).5 else 0.0,0.0)}
 @Test fun prefersRight(){assertEquals(Cell(2,3),MovementPlanner.choose(Cell(2,2),board(),emptyList())?.target)}
 @Test fun routesToItem(){val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(1,2))),emptyList());assertEquals(Cell(1,2),a?.target);assertEquals(ActionKind.MOVE,a?.kind)}

 /** Freie Zeile: es werden gleich mehrere Rechtsschritte am Stueck geplant. */
 @Test fun burstsThreeStepsOnClearRow(){
  val p=MovementPlanner.plan(Cell(2,0),board(),emptyList(),maxSteps=3)
  assertEquals(listOf(Cell(2,1),Cell(2,2),Cell(2,3)),p.map{it.target})
 }

 /** Verschluckt das Spiel einen Tap, verschiebt sich die Kette um ein Feld. Steht in dieser
  *  Sicherheitsspalte eine Pyramide, wird nicht gebuendelt - sonst ginge eine Kralle verloren. */
 @Test fun noBurstWhenPyramidCouldBeHitAfterMissedTap(){
  val p=MovementPlanner.plan(Cell(2,0),board(setOf(Cell(2,3))),emptyList(),claws=5,maxSteps=3)
  assertEquals(1,p.size)
 }

 /** Zum Collectable wird der Pfad gebuendelt, nicht nur der erste Schritt. */
 @Test fun burstsAlongItemRoute(){
  val p=MovementPlanner.plan(Cell(2,0),board(items=setOf(Cell(2,3))),emptyList(),maxSteps=2)
  assertEquals(listOf(Cell(2,1),Cell(2,2)),p.map{it.target})
 }

 /** Zerschlagen wird nie gebuendelt - danach muss das Bild neu bewertet werden. */
 @Test fun neverBurstsAttacks(){
  val walls=setOf(Cell(2,2),Cell(1,1),Cell(1,2),Cell(1,3),Cell(3,1),Cell(3,2),Cell(3,3))
  val p=MovementPlanner.plan(Cell(2,1),board(walls,setOf(Cell(2,3))),emptyList(),claws=3,maxSteps=3)
  assertEquals(1,p.size);assertEquals(ActionKind.ATTACK,p[0].kind)
 }

 /** Auf dem Weg zum Item wird nur zerschlagen, wenn es keinen Bogen darum herum gibt. Hier sind
  *  die Nachbarzeilen zugebaut, der Durchbruch ist also der einzige bezahlbare Weg. */
 @Test fun attacksBlockingPyramidOnItemRouteWhenWalledIn(){
  val walls=setOf(Cell(2,2),Cell(1,1),Cell(1,2),Cell(1,3),Cell(3,1),Cell(3,2),Cell(3,3))
  val a=MovementPlanner.choose(Cell(2,1),board(walls,setOf(Cell(2,3))),emptyList(),claws=3)
  assertEquals(Cell(2,2),a?.target);assertEquals(ActionKind.ATTACK,a?.kind)
 }

 /** Steht ein kurzer Bogen zur Verfuegung, bleibt die Pyramide stehen - auch mit vollem Vorrat. */
 @Test fun prefersDetourOverBreakingWithManyClaws(){
  val a=MovementPlanner.choose(Cell(2,1),board(setOf(Cell(2,2)),setOf(Cell(2,3))),emptyList(),claws=5)
  assertEquals(ActionKind.MOVE,a?.kind)
 }

 /** Mit nur der Reserve wird die Pyramide rechts umgangen statt zerschlagen. */
 @Test fun avoidsPyramidWithOnlyReserveClaw(){
  val a=MovementPlanner.choose(Cell(2,2),board(setOf(Cell(2,3))),emptyList(),claws=1)
  assertEquals(ActionKind.MOVE,a?.kind);assertNotEquals(Direction.RIGHT,a?.direction)
 }

 /** Unbekannte Zahl wird genauso vorsichtig behandelt wie "fast leer". */
 @Test fun avoidsPyramidWhenCountUnknown(){
  assertEquals(ActionKind.MOVE,MovementPlanner.choose(Cell(2,2),board(setOf(Cell(2,3))),emptyList(),claws=null)?.kind)
 }

 /** Ist wirklich kein Feld mehr frei, wird die Reservekralle eingesetzt. */
 @Test fun spendsReserveWhenCompletelyBoxedIn(){
  val walls=setOf(Cell(1,2),Cell(3,2),Cell(2,1),Cell(2,3))
  val a=MovementPlanner.choose(Cell(2,2),board(walls),emptyList(),claws=1)
  assertEquals(ActionKind.ATTACK,a?.kind);assertTrue(a?.reason.orEmpty().contains("Reserve"))
 }

 /** Ohne Krallen ist eine Pyramide eine Wand - dann lieber gar kein Zug als ein wirkungsloser Tap. */
 @Test fun neverAttacksWithoutClaws(){
  val walls=setOf(Cell(1,2),Cell(3,2),Cell(2,1),Cell(2,3))
  assertNull(MovementPlanner.choose(Cell(2,2),board(walls),emptyList(),claws=0))
 }

 @Test fun breaksOscillationAtRightEdge(){val h=listOf(Cell(4,3),Cell(4,4),Cell(4,3),Cell(4,4));assertEquals(Direction.UP,MovementPlanner.choose(Cell(4,4),board(),h)?.direction)}

 /** Bei freier Bahn zaehlt die Vorausschau: die Zeile mit drei freien Feldern gewinnt. */
 @Test fun prefersRowWithClearLookahead(){
  val blockedRow=setOf(Cell(2,2),Cell(2,3),Cell(2,4))
  val a=MovementPlanner.choose(Cell(2,1),board(blockedRow),emptyList())
  assertNotEquals(Direction.RIGHT,a?.direction)
 }

 /** Ohne Blockade und ohne Reserve darf kein Dash verschwendet werden. */
 @Test fun noDashWithoutReason(){
  val b=board(setOf(Cell(2,2),Cell(2,3)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),b,emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Schon zwei Pyramiden in den naechsten drei Feldern und mehr als zwei Ladungen: Dash. */
 @Test fun dashOverThreePyramidsWithReserve(){
  val b=board(setOf(Cell(2,1),Cell(2,2)))
  assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),b,emptyList(),dashAvailable=true,dashCharges=3)?.kind)
 }

 /** Sichtbare Energie hat Vorrang; der Bot darf nicht daran vorbeidashen. */
 @Test fun noDashPastVisibleEnergy(){
  val b=board(setOf(Cell(2,1),Cell(2,2)),setOf(Cell(4,4)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),b,emptyList(),dashAvailable=true,dashCharges=3)?.kind)
 }

 /** Festgefahren darf immer gedasht werden, auch ohne Reserve. */
 @Test fun dashWhenStuck(){
  assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,2),board(),emptyList(),dashAvailable=true,stuck=true)?.kind)
 }

 /** Ohne verfuegbaren Dash bleibt es beim normalen Zug. */
 @Test fun noDashWhenUnavailable(){
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,2),board(),emptyList(),dashAvailable=false,stuck=true)?.kind)
 }
}
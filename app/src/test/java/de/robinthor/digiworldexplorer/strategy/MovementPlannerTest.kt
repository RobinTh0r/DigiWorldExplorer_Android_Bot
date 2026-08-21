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
  val walls=(0..4).map{Cell(it,2)}.toSet()
  val p=MovementPlanner.plan(Cell(2,1),board(walls,setOf(Cell(2,3))),emptyList(),claws=3,maxSteps=3)
  assertEquals(1,p.size);assertEquals(ActionKind.ATTACK,p[0].kind)
 }

 /** Auf dem Weg zum Item wird nur zerschlagen, wenn es keinen Bogen darum herum gibt. Hier sind
  *  die Nachbarzeilen zugebaut, der Durchbruch ist also der einzige bezahlbare Weg. */
 @Test fun attacksBlockingPyramidOnItemRouteWhenWalledIn(){
  val walls=(0..4).map{Cell(it,2)}.toSet()
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

 /** Eine Dash-Ladung bleibt als Reserve fuer einen echten Stuck-Zustand erhalten. */
 @Test fun noDashWithoutReason(){
  val b=board(setOf(Cell(2,2),Cell(2,3)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),b,emptyList(),dashAvailable=true,dashCharges=1)?.kind)
 }

 /** Auch bei zwei Pyramiden wird nicht gedasht, solange ein freier Umweg existiert. */
 @Test fun noDashWhenTwoPyramidsHaveDetour(){
  val b=board(setOf(Cell(2,1),Cell(2,2)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),b,emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Zwei Pyramiden voraus und eine vollstaendige Wand: jetzt ist Dash wirklich erzwungen. */
 @Test fun dashOnlyWhenWallHasNoDetour(){
  val walls=(0..4).flatMap{r->listOf(Cell(r,1),Cell(r,2))}.toSet()
  assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),board(walls),emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Sichtbare Energie hat Vorrang; der Bot darf nicht daran vorbeidashen. */
 @Test fun noDashPastVisibleEnergy(){
  val b=board(setOf(Cell(2,1),Cell(2,2)),setOf(Cell(4,4)))
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,0),b,emptyList(),dashAvailable=true,dashCharges=2)?.kind)
 }

 /** Positionspendeln allein verbraucht keinen Dash, wenn weiterhin ein freier Weg existiert. */
 @Test fun noDashWhenStuckButFreeRouteExists(){
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,2),board(),emptyList(),dashAvailable=true,stuck=true)?.kind)
 }

 /** Ohne verfuegbaren Dash bleibt es beim normalen Zug. */
 @Test fun noDashWhenUnavailable(){
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,2),board(),emptyList(),dashAvailable=false,stuck=true)?.kind)
 }

 /** Ein langer freier Umweg gewinnt immer gegen einen kurzen Angriffspfad. */
 @Test fun neverAttacksWhenLongDetourExists(){
  val walls=setOf(Cell(2,1),Cell(1,1),Cell(3,1))
  val a=MovementPlanner.choose(Cell(2,0),board(walls,setOf(Cell(2,2))),emptyList(),claws=5)
  assertEquals(ActionKind.MOVE,a?.kind)
  assertNotEquals(Cell(2,1),a?.target)
 }

 /** In einer Drei-Felder-Tasche folgt der Bot dem Ausbruchspfad und greift an statt zu pendeln. */
 @Test fun attacksOutOfClosedPocket(){
  val wall=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(4,1),board(wall),emptyList(),claws=1)
  assertEquals(ActionKind.ATTACK,a?.kind)
  assertEquals(Cell(4,2),a?.target)
 }

 /** Ohne Kralle bleibt in einer vollstaendigen Tasche nur der bestaetigte Notfall-Dash. */
 @Test fun emergencyDashWhenTrappedWithoutClaw(){
  val wall=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(2,1),board(wall),emptyList(),dashAvailable=true,dashCharges=1,claws=0)
  assertEquals(ActionKind.DASH,a?.kind)
 }

 /** Gleichwertige Items vor dem Spieler werden vor einem naeheren Rueckweg eingesammelt. */
 @Test fun prefersForwardItemOverBacktracking(){
  val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(2,1),Cell(2,4))),emptyList())
  assertEquals(Direction.RIGHT,a?.direction)
 }

 /** In einer Tasche wird erst ein erreichbares Item genommen und danach gezielt ausgebrochen. */
 @Test fun collectsPocketItemBeforeBreakingOut(){
  val wall=(0..4).map{Cell(it,2)}.toSet()
  val a=MovementPlanner.choose(Cell(4,1),board(wall,setOf(Cell(3,1))),emptyList(),claws=1)
  assertEquals(ActionKind.MOVE,a?.kind)
  assertEquals(Cell(3,1),a?.target)
 }

 /** Eine Seitenzelle ohne eigenen Ausgang wird nicht betreten, wenn der Aussenweg sichtbar ist. */
 @Test fun neverEntersVisibleCornerBeforeDetour(){
  val player=Cell(3,1)
  val walls=setOf(Cell(3,2),Cell(2,2),Cell(4,0),Cell(4,2))
  val base=board(walls).toMutableMap()
  base[Cell(4,1)]=CellScores(0.0,0.0,0.0,0.0,0.0,0.0,1.0)
  val a=MovementPlanner.choose(player,base,emptyList(),claws=4)
  assertEquals(Direction.UP,a?.direction)
  assertNotEquals(Cell(4,1),a?.target)
 }
 /** Ein benachbartes Collectable gewinnt gegen ein weiter rechts liegendes, aber ferneres Ziel. */
 @Test fun adjacentItemBeatsFarForwardItem(){
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(1,1),Cell(2,4))),emptyList())
  assertEquals(Direction.UP,a?.direction)
  assertEquals(Cell(1,1),a?.target)
 }

 @Test fun forwardProfileNeverMovesLeft(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true)
  val a=MovementPlanner.choose(Cell(2,2),board(items=setOf(Cell(2,1))),emptyList(),settings=settings)
  assertEquals(Direction.RIGHT,a?.direction)
 }

 @Test fun forwardProfileAttacksObstacleDirectlyAhead(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true)
  val a=MovementPlanner.choose(Cell(2,1),board(obstacles=setOf(Cell(2,2))),emptyList(),claws=2,settings=settings)
  assertEquals(ActionKind.ATTACK,a?.kind)
  assertEquals(Cell(2,2),a?.target)
  assertEquals(Direction.RIGHT,a?.direction)
 }

 @Test fun forwardProfileStillTargetsVisibleEnergy(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true)
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(1,1))),emptyList(),settings=settings)
  assertEquals(ActionKind.MOVE,a?.kind)
  assertEquals(Cell(1,1),a?.target)
  assertEquals(Direction.UP,a?.direction)
 }

 @Test fun dashSpamUsesOnlyConfirmedPositiveCounter(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true,dashSpamUntilZero=true)
  assertEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),board(),emptyList(),dashAvailable=true,dashCharges=2,settings=settings)?.kind)
  assertNotEquals(ActionKind.DASH,MovementPlanner.choose(Cell(2,1),board(),emptyList(),dashAvailable=true,dashCharges=0,settings=settings)?.kind)
 }

 @Test fun visibleEnergySuppressesDashSpam(){
  val settings=DwsNavigationSettings(allowLeft=false,forceForwardAttack=true,dashSpamUntilZero=true)
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(1,1))),emptyList(),dashAvailable=true,dashCharges=3,settings=settings)
  assertNotEquals(ActionKind.DASH,a?.kind)
  assertEquals(Cell(1,1),a?.target)
 }

 @Test fun betterCollectTakesSameColumnEnergyBeforeCloserForwardEnergy(){
  val settings=DwsNavigationSettings(collectOnlyEnergy=true,betterEnergyCollect=true)
  val a=MovementPlanner.choose(Cell(2,1),board(items=setOf(Cell(0,1),Cell(2,2))),emptyList(),settings=settings)
  assertEquals(Direction.UP,a?.direction)
  assertEquals(Cell(1,1),a?.target)
 }

 @Test fun onlyEnergyIgnoresOtherCollectables(){
  val settings=DwsNavigationSettings(collectOnlyEnergy=true)
  val cells=board(items=setOf(Cell(1,1))).toMutableMap()
  cells[Cell(1,1)]=cells.getValue(Cell(1,1)).copy(orange=0.0)
  val a=MovementPlanner.choose(Cell(2,1),cells,emptyList(),settings=settings)
  assertNotEquals(Cell(1,1),a?.target)
  assertEquals(Direction.RIGHT,a?.direction)
 }
}

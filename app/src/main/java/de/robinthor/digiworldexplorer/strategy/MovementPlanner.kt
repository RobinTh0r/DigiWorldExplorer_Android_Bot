package de.robinthor.digiworldexplorer.strategy

import de.robinthor.digiworldexplorer.detection.Cell
import de.robinthor.digiworldexplorer.detection.CellScores
import java.util.PriorityQueue

enum class Direction(val dr:Int,val dc:Int){RIGHT(0,1),DOWN(1,0),UP(-1,0),LEFT(0,-1)}
enum class ActionKind{MOVE,ATTACK,DASH}
data class Action(val kind:ActionKind,val target:Cell,val direction:Direction,val reason:String)

object MovementPlanner{
 /** Pyramiden sind keine Sackgasse: Mit Krallen zerstoert das Hineinlaufen das Hindernis.
  *  Das kostet eine Kralle, einen zusaetzlichen Tap und die Zerschlag-Animation, deshalb sind
  *  Umwege bis zu drei Feldern guenstiger als ein Durchbruch. */
 private const val OBSTACLE_COST=4
 /** So viele Krallen bleiben als Reserve fuer eine echte Sackgasse liegen. Solange nicht mehr
  *  als das vorhanden ist, wird jede Pyramide umgangen - auch auf grossen Umwegen. */
 const val CLAW_RESERVE=1
 /** So weit plant der Bot nach rechts voraus, wenn kein Collectable sichtbar ist. */
 const val LOOKAHEAD=3
 /** Dash lohnt sich erst bei mehr als so vielen Pyramiden in den naechsten [LOOKAHEAD] Feldern. */
 private const val DASH_PYRAMIDS=2
 /** Ausserhalb einer Blockade nur dashen, wenn mehr als so viele Ladungen uebrig sind. */
 private const val DASH_RESERVE=1
 /** Gewicht der Vorausschau je freiem Feld. Muss gross genug sein, um den Rechts-Bonus zu
  *  ueberstimmen, wenn die Zeile zugebaut ist. */
 private const val LOOKAHEAD_WEIGHT=20
 /** Abschlag fuer ein Feld, das erst zerschlagen werden muss. Muss den Rechts-Bonus klar
  *  ueberwiegen, sonst rammt der Bot die Pyramide, obwohl eine Zeile weiter alles frei ist. */
 private const val BLOCKED_PENALTY=90
 /** Ab diesem Wert gilt eine Zelle als Collectable. */
 private const val ITEM_SCORE=.06
 /** Ab diesem Orangeanteil ist das Collectable eine Energiekugel und keine Kralle. */
 private const val ENERGY_ORANGE=.06
 /** Energiekugeln verschwinden von selbst wieder, Krallen bleiben liegen. Ein Umweg von bis zu
  *  so vielen Feldern lohnt sich deshalb, um die Energie zuerst einzusammeln. */
 private const val ENERGY_DETOUR=3
 /** Abschlag fuer ein Feld, von dem aus die rechte Spalte nicht mehr erreichbar ist. Muss den
  *  Rechts-Bonus samt Vorausschau ueberwiegen, sonst laeuft der Bot weiter in die Tasche. */
 private const val DEAD_END_PENALTY=250

 private data class Node(val cost:Int,val cell:Cell,val path:List<Action>):Comparable<Node>{override fun compareTo(o:Node)=cost.compareTo(o.cost)}

 /**
  * @param claws gelesener Krallenvorrat, `null` wenn die Zahl nicht sicher gelesen wurde. Beides
  *  wird gleich vorsichtig behandelt: Zerschlagen ist dann nur erlaubt, wenn sonst gar nichts geht.
  */
 fun choose(
  player:Cell,
  cells:Map<Cell,CellScores>,
  history:List<Cell>,
  dashAvailable:Boolean=false,
  preview:Map<Cell,CellScores> = emptyMap(),
  forbiddenObstacles:Set<Cell> = emptySet(),
  dashCharges:Int=0,
  stuck:Boolean=false,
  claws:Int?=null,
  settings:DwsNavigationSettings=DwsNavigationSettings(),
 ):Action?{
  val energyVisible=cells.any{(cell,score)->cell!=player&&score.orange>ENERGY_ORANGE}
  // The optional Supporter test profile deliberately spends every *confirmed* dash charge when no
  // energy is visible. AutoMoveController supplies zero when the HUD counter is unreadable, so this
  // branch can never spam an assumed fallback value.
  if(settings.dashSpamUntilZero&&!energyVisible&&dashAvailable&&dashCharges>0){
   return Action(ActionKind.DASH,Cell(player.row,minOf(4,player.col+LOOKAHEAD)),Direction.RIGHT,"Dash-Spam bis HUD 0")
  }

  // Always search the complete visible board without spending a resource first. A long detour is
  // still preferable to attacking or dashing; resource use is considered only if this search
  // cannot reach an item or the right side at all.
  route(player,cells,history,preview,forbiddenObstacles,false,settings)?.let{return it}

  val ahead=(1..LOOKAHEAD).map{Cell(player.row,player.col+it)}.filter{it.col<=4}
  val pyramidsAhead=ahead.count{cells[it]?.obstacle()==true}
  if(dashAvailable&&dashCharges>DASH_RESERVE&&pyramidsAhead>=DASH_PYRAMIDS&&!energyVisible){
   return Action(ActionKind.DASH,Cell(player.row,minOf(4,player.col+LOOKAHEAD)),Direction.RIGHT,"erzwungener Ausweg: $pyramidsAhead Pyramiden")
  }

  // There is no free route. Prefer one deliberate attack along the cheapest breakout path.
  if(claws==null||claws>0){
   route(player,cells,history,preview,forbiddenObstacles,true,settings)?.let{action->
    return if(action.kind==ActionKind.ATTACK)action.copy(reason=if(claws!=null&&claws<=CLAW_RESERVE) "Reserve: kein freier Weg drumherum" else "kein freier Weg drumherum") else action
   }
  }

  // With no usable claw, a confirmed dash is the final escape option. Never dash merely because
  // the short-term position history oscillates while a normal route still exists.
  if(dashAvailable&&dashCharges>0){
   return Action(ActionKind.DASH,Cell(player.row,minOf(4,player.col+LOOKAHEAD)),Direction.RIGHT,"Notausgang ohne Kralle")
  }
  return null
 }
 private fun allowedDirections(settings:DwsNavigationSettings):List<Direction> = if(settings.allowLeft) Direction.entries else Direction.entries.filterNot{it==Direction.LEFT}

 private fun route(
  player:Cell,
  cells:Map<Cell,CellScores>,
  history:List<Cell>,
  preview:Map<Cell,CellScores>,
  forbiddenObstacles:Set<Cell>,
  mayBreak:Boolean,
  settings:DwsNavigationSettings,
 ):Action?{
  val obstacles=cells.filterValues{it.obstacle()}.keys
  val blockedCells=if(mayBreak)forbiddenObstacles else forbiddenObstacles+obstacles

  // Forward-only test profile: a reachable energy target still wins. Otherwise the planner keeps
  // the current row and either moves right or, during the resource-backed pass, attacks the pyramid
  // directly in front. No generic item route can pull the player sideways or backwards here.
  if(settings.forceForwardAttack){
   val energy=cells.filter{(cell,score)->cell!=player&&score.orange>ENERGY_ORANGE}.keys
   if(settings.betterEnergyCollect){
    val sameColumn=energy.filter{it.col==player.col}.toSet()
    forwardItemPath(player,sameColumn,cells,blockedCells,settings)?.firstOrNull()?.let{return it.copy(reason="Energie in eigener Spalte zuerst")}
   }
   forwardItemPath(player,energy,cells,blockedCells,settings)?.firstOrNull()?.let{return it.copy(reason="Energie direkt")}
   val right=Cell(player.row,player.col+1)
   cells[right]?.let{score->
    if(right !in forbiddenObstacles&&(!score.obstacle()||mayBreak)){
     return Action(if(score.obstacle())ActionKind.ATTACK else ActionKind.MOVE,right,Direction.RIGHT,if(score.obstacle())"Vorwaertshindernis angreifen" else "Nur vorwaerts")
    }
   }
   return null
  }
  // Once the free search has proved that the player is trapped, follow one stable breakout path
  // toward the right edge. This prevents wandering around the remaining two or three free cells.
  if(mayBreak){
   val rightEdge=cells.keys.filter{it.col==4}.toSet()
   shortestPath(player,rightEdge,cells,forbiddenObstacles,settings)?.firstOrNull()?.let{return it.copy(reason="Ausbruchspfad")}
  }

  itemPath(player,cells,blockedCells,settings)?.let{path->
   val goal=path.last().target
   val energy=(cells[goal]?.orange?:0.0)>ENERGY_ORANGE
   return path.first().copy(reason=if(energy)"Energie zuerst" else "item route")
  }

  // If no item remains inside the reachable component and the right edge cannot be reached without
  // a pyramid, report failure so choose() may consider exactly one resource-backed escape.
  if(!mayBreak&&!escapesRight(player,cells,blockedCells,false,settings))return null

  val oscillating=history.size>=4&&history.takeLast(4).let{it[0]==it[2]&&it[1]==it[3]}
  val previous=history.dropLast(1).lastOrNull()
  val candidates=allowedDirections(settings).mapNotNull{d->
   val n=Cell(player.row+d.dr,player.col+d.dc);val scoreCell=cells[n]?:return@mapNotNull null;if(n in blockedCells)return@mapNotNull null
   val blocked=scoreCell.obstacle()
   var score=when(d){Direction.RIGHT->100;Direction.DOWN->20;Direction.UP->15;Direction.LEFT->-40}
   if(!blocked)score+=(scoreCell.highlight*20).toInt()
   score+=freeAhead(n,cells)*LOOKAHEAD_WEIGHT
   if(blocked)score-=BLOCKED_PENALTY
   if(!blocked&&!escapesRight(n,cells,blockedCells+player,mayBreak,settings))score-=DEAD_END_PENALTY
   if(n==previous)score-=80;if(oscillating&&n==history[history.lastIndex-1])score-=200
   val look=preview[Cell(n.row,5)];if(look!=null&&look.pyramid>.17&&look.item<=.06)score-=35;if((look?.item?:0.0)>.06)score+=45
   score to Action(if(blocked)ActionKind.ATTACK else ActionKind.MOVE,n,d,if(blocked)"Pyramide zerschlagen" else "Strecke rechts")
  }
  // Eine sichtbare Sackgasse ist keine "schlechte" Route, sondern gar keine Route. Sobald
  // wenigstens ein Nachbar weiterhin zur rechten Brettkante fuehrt, werden alle Taschen hart
  // ausgeschlossen. So laeuft die Figur nicht erst in die Ecke und danach denselben Weg zurueck.
  val escaping=candidates.filter{(_,action)->action.kind!=ActionKind.MOVE||escapesRight(action.target,cells,blockedCells+player,mayBreak,settings)}
  return (escaping.ifEmpty{candidates}).maxByOrNull{it.first}?.second
 }
 /** Wie viele der naechsten [LOOKAHEAD] Felder rechts von [from] frei begehbar sind, normiert
  *  auf [LOOKAHEAD]. Am rechten Rand stehen weniger Felder zur Verfuegung - ohne Normierung
  *  wuerde die Kante sonst systematisch schlechter bewertet als die Brettmitte. */
 private fun freeAhead(from:Cell,cells:Map<Cell,CellScores>):Int{
  val ahead=(1..LOOKAHEAD).map{Cell(from.row,from.col+it)}.filter{it.col<=4}
  if(ahead.isEmpty())return LOOKAHEAD
  return ahead.count{cells[it]?.obstacle()!=true}*LOOKAHEAD/ahead.size
 }

 /**
  * Weg zum naechsten Collectable - Energiekugeln zuerst. Sie verschwinden nach kurzer Zeit von
  * selbst, waehrend Krallen liegen bleiben. Ohne diese Bevorzugung nahm der Bot immer nur das
  * naechstgelegene Symbol und lief an einer bereits sichtbaren Energie vorbei, bis sie weg war.
  */
 private fun itemPath(player:Cell,cells:Map<Cell,CellScores>,blocked:Set<Cell>,settings:DwsNavigationSettings):List<Action>?{
  val items=cells.filter{(cell,s)->
   cell!=player&&s.item>ITEM_SCORE&&(!settings.collectOnlyEnergy||s.orange>ENERGY_ORANGE)
  }.keys
  if(items.isEmpty())return null
  val any=forwardItemPath(player,items,cells,blocked,settings)
  val energy=items.filter{(cells[it]?.orange?:0.0)>ENERGY_ORANGE}.toSet()
  if(energy.isEmpty())return any
  if(settings.betterEnergyCollect){
   val sameColumn=energy.filter{it.col==player.col}.toSet()
   forwardItemPath(player,sameColumn,cells,blocked,settings)?.let{return it}
  }
  val toEnergy=forwardItemPath(player,energy,cells,blocked,settings)?:return any
  if(any==null)return toEnergy
  return if(toEnergy.size<=any.size+ENERGY_DETOUR)toEnergy else any
 }

 /**
  * Bei gleichartigen Items gewinnt nicht stumpf der kuerzeste Einzelweg. Ein Ziel links vom
  * Spieler bekommt eine deutliche Ruecklauf-Strafe, waehrend Ziele weiter rechts bevorzugt
  * werden. Dadurch sammelt der Bot eine sichtbare Reihe in Laufrichtung ein, statt fuer ein
  * nahes Item umzudrehen und die vorderen Items durch den Bildlauf zu verlieren.
  */
 private fun forwardItemPath(player:Cell,targets:Set<Cell>,cells:Map<Cell,CellScores>,blocked:Set<Cell>,settings:DwsNavigationSettings):List<Action>? =
  targets.mapNotNull{target->
   shortestPath(player,setOf(target),cells,blocked,settings)?.let{path->
    val backwards=(player.col-target.col).coerceAtLeast(0)
    // Entfernung bleibt das Hauptkriterium; rechts entscheidet nur bei gleichem Wegwert.
    Triple(path.size+backwards*6,-target.col,path)
   }
  }.minWithOrNull(compareBy<Triple<Int,Int,List<Action>>>{it.first}.thenBy{it.second})?.third

 /**
  * Ist von [from] aus die rechte Spalte ueberhaupt noch erreichbar? Nur so laesst sich eine von
  * Pyramiden umschlossene Tasche von einem normalen Umweg unterscheiden - das Brett ist mit 5x5
  * klein genug, dass die vollstaendige Suche billiger ist als jede Heuristik.
  */
 private fun escapesRight(from:Cell,cells:Map<Cell,CellScores>,blocked:Set<Cell>,passObstacles:Boolean,settings:DwsNavigationSettings):Boolean{
  if(from.col>=4)return true
  val seen=mutableSetOf(from);val queue=ArrayDeque(listOf(from))
  while(queue.isNotEmpty()){
   val c=queue.removeFirst()
   if(c.col>=4)return true
   for(d in allowedDirections(settings)){
    val n=Cell(c.row+d.dr,c.col+d.dc)
    if(n in seen||n in blocked)continue
    val s=cells[n]?:continue
    if(s.obstacle()&&!passObstacles)continue
    seen+=n;queue+=n
   }
  }
  return false
 }

 private fun shortestPath(start:Cell,targets:Set<Cell>,cells:Map<Cell,CellScores>,forbidden:Set<Cell>,settings:DwsNavigationSettings):List<Action>?{
  val q=PriorityQueue<Node>();q+=Node(0,start,emptyList());val best=mutableMapOf(start to 0)
  while(q.isNotEmpty()){val n=q.remove();if(n.cost!=best[n.cell])continue;if(n.cell in targets&&n.path.isNotEmpty())return n.path
   for(d in allowedDirections(settings)){val next=Cell(n.cell.row+d.dr,n.cell.col+d.dc);if(next in forbidden)continue;val s=cells[next]?:continue;val obstacle=s.obstacle();val cost=n.cost+(if(obstacle)OBSTACLE_COST else 1)
    if(cost<(best[next]?:999)){best[next]=cost;q+=Node(cost,next,n.path+Action(if(obstacle)ActionKind.ATTACK else ActionKind.MOVE,next,d,"item route"))}}}
  return null
 }

 /**
  * Wie [choose], liefert aber zusaetzlich die naechsten Schritte derselben Route, damit sie ohne
  * erneute Bildauswertung hintereinander getippt werden koennen.
  *
  * Gebuendelt wird nur, wenn ein Fehltritt folgenlos bliebe: Verschluckt das Spiel einen Tap,
  * verschiebt sich die ganze Kette um ein Feld. Deshalb muss der Korridor inklusive einer
  * Sicherheitsspalte frei von Pyramiden sein - sonst wuerde ein verschluckter Tap eine Kralle
  * verbrauchen. Im Zweifel bleibt es bei einem einzelnen Schritt.
  */
 fun plan(
  player:Cell,
  cells:Map<Cell,CellScores>,
  history:List<Cell>,
  dashAvailable:Boolean=false,
  preview:Map<Cell,CellScores> = emptyMap(),
  forbiddenObstacles:Set<Cell> = emptySet(),
  dashCharges:Int=0,
  stuck:Boolean=false,
  claws:Int?=null,
  maxSteps:Int=1,
  settings:DwsNavigationSettings=DwsNavigationSettings(),
 ):List<Action>{
  val first=choose(player,cells,history,dashAvailable,preview,forbiddenObstacles,dashCharges,stuck,claws,settings)?:return emptyList()
  val single=listOf(first)
  if(maxSteps<=1||first.kind!=ActionKind.MOVE)return single
  val mayBreak=claws!=null&&claws>CLAW_RESERVE
  val blocked=forbiddenObstacles+(if(mayBreak)emptySet() else cells.filterValues{it.obstacle()}.keys)
   val itemRoute=itemPath(player,cells,blocked,settings)
   val route=if(itemRoute!=null){
    if(itemRoute.first().target!=first.target)return single
    itemRoute.takeWhile{it.kind==ActionKind.MOVE}.take(maxSteps)
  } else {
   if(first.direction!=Direction.RIGHT)return single
   val out=mutableListOf(first)
   while(out.size<maxSteps){
    val n=Cell(first.target.row,out.last().target.col+1)
    if(n.col>4)break
    val s=cells[n]?:break
    if(s.obstacle()||n in blocked)break
    out+=Action(ActionKind.MOVE,n,Direction.RIGHT,"Strecke rechts")
   }
   out
  }
  if(route.size<=1)return single
  return if(corridorClear(player,route,cells))route else single
 }

 /** Alle Felder der Route plus je eine Spalte davor und dahinter muessen frei sein. */
 private fun corridorClear(player:Cell,route:List<Action>,cells:Map<Cell,CellScores>):Boolean{
  val rows=(route.map{it.target.row}+player.row).toSet()
  val cols=(route.map{it.target.col}+player.col)
  return rows.all{r->(cols.min()..cols.max()+1).all{c->c>4||cells[Cell(r,c)]?.obstacle()!=true}}
 }
}

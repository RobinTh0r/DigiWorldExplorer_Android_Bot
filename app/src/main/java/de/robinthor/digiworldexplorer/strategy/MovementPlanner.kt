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
 ):Action?{
  // Ohne Krallen ist eine Pyramide eine echte Wand. Mit nur der Reserve wird sie zwar nicht
  // angefasst, bleibt aber als Notausgang bestehen - deshalb erst der Versuch ohne Zerschlagen.
  val mayBreak=claws!=null&&claws>CLAW_RESERVE
  route(player,cells,history,dashAvailable,preview,forbiddenObstacles,dashCharges,stuck,mayBreak)?.let{return it}
  if(mayBreak)return null
  // Kein Weg drumherum. Jetzt zaehlt nur noch, ob ueberhaupt eine Kralle da ist.
  if(claws!=null&&claws<=0)return null
  return route(player,cells,history,dashAvailable,preview,forbiddenObstacles,dashCharges,stuck,true)
   ?.let{if(it.kind==ActionKind.ATTACK)it.copy(reason="Reserve: kein Weg drumherum") else it}
 }

 private fun route(
  player:Cell,
  cells:Map<Cell,CellScores>,
  history:List<Cell>,
  dashAvailable:Boolean,
  preview:Map<Cell,CellScores>,
  forbiddenObstacles:Set<Cell>,
  dashCharges:Int,
  stuck:Boolean,
  mayBreak:Boolean,
 ):Action?{
  val blockedCells=if(mayBreak)forbiddenObstacles else forbiddenObstacles+cells.filterValues{it.obstacle()}.keys
  itemPath(player,cells,blockedCells)?.let{path->
   val goal=path.last().target
   val energy=(cells[goal]?.orange?:0.0)>ENERGY_ORANGE
   return path.first().copy(reason=if(energy)"Energie zuerst" else "item route")
  }
  // Kein Collectable in Sicht: Strecke machen und dabei [LOOKAHEAD] Felder vorausplanen.
  val ahead=(1..LOOKAHEAD).map{Cell(player.row,player.col+it)}.filter{it.col<=4}
  val pyramidsAhead=ahead.count{cells[it]?.obstacle()==true}
  val energyVisible=cells.any{(cell,score)->cell!=player&&score.orange>ENERGY_ORANGE}
  val dashReason=when{
   !dashAvailable->null
   stuck->"festgefahren"
   !energyVisible&&dashCharges>DASH_RESERVE&&pyramidsAhead>=DASH_PYRAMIDS&&player.col+LOOKAHEAD<=4->"$pyramidsAhead Pyramiden voraus, $dashCharges Ladungen"
   else->null
  }
  if(dashReason!=null)return Action(ActionKind.DASH,Cell(player.row,minOf(4,player.col+LOOKAHEAD)),Direction.RIGHT,dashReason)

  val oscillating=history.size>=4&&history.takeLast(4).let{it[0]==it[2]&&it[1]==it[3]}
  val previous=history.dropLast(1).lastOrNull()
  return Direction.entries.mapNotNull{d->
   val n=Cell(player.row+d.dr,player.col+d.dc);val s=cells[n]?:return@mapNotNull null;if(n in blockedCells)return@mapNotNull null
   val blocked=s.obstacle()
   var score=when(d){Direction.RIGHT->100;Direction.DOWN->20;Direction.UP->15;Direction.LEFT->-40}
   // Der Highlight-Anteil ueberschneidet sich farblich mit der Pyramide, deshalb nur auf freien Feldern werten.
   if(!blocked)score+=(s.highlight*20).toInt()
   score+=freeAhead(n,cells)*LOOKAHEAD_WEIGHT
   if(blocked)score-=BLOCKED_PENALTY
   // Die Vorausschau sieht nur die eigene Zeile. Eine von Pyramiden umschlossene Tasche wirkt
   // darin voellig frei - der Bot lief hinein und musste den ganzen Weg zurueck.
   if(!blocked&&!escapesRight(n,cells,blockedCells,mayBreak))score-=DEAD_END_PENALTY
   if(n==previous)score-=80;if(oscillating&&n==history[history.lastIndex-1])score-=200
   val look=preview[Cell(n.row,5)];if(look!=null&&look.pyramid>.17&&look.item<=.06)score-=35;if((look?.item?:0.0)>.06)score+=45
   score to Action(if(blocked)ActionKind.ATTACK else ActionKind.MOVE,n,d,if(blocked)"Pyramide zerschlagen" else "Strecke rechts")
  }.maxByOrNull{it.first}?.second
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
 private fun itemPath(player:Cell,cells:Map<Cell,CellScores>,blocked:Set<Cell>):List<Action>?{
  val items=cells.filter{(cell,s)->cell!=player&&s.item>ITEM_SCORE}.keys
  if(items.isEmpty())return null
  val any=shortestPath(player,items,cells,blocked)
  val energy=items.filter{(cells[it]?.orange?:0.0)>ENERGY_ORANGE}.toSet()
  if(energy.isEmpty())return any
  val toEnergy=shortestPath(player,energy,cells,blocked)?:return any
  if(any==null)return toEnergy
  return if(toEnergy.size<=any.size+ENERGY_DETOUR)toEnergy else any
 }

 /**
  * Ist von [from] aus die rechte Spalte ueberhaupt noch erreichbar? Nur so laesst sich eine von
  * Pyramiden umschlossene Tasche von einem normalen Umweg unterscheiden - das Brett ist mit 5x5
  * klein genug, dass die vollstaendige Suche billiger ist als jede Heuristik.
  */
 private fun escapesRight(from:Cell,cells:Map<Cell,CellScores>,blocked:Set<Cell>,passObstacles:Boolean):Boolean{
  if(from.col>=4)return true
  val seen=mutableSetOf(from);val queue=ArrayDeque(listOf(from))
  while(queue.isNotEmpty()){
   val c=queue.removeFirst()
   if(c.col>=4)return true
   for(d in Direction.entries){
    val n=Cell(c.row+d.dr,c.col+d.dc)
    if(n in seen||n in blocked)continue
    val s=cells[n]?:continue
    if(s.obstacle()&&!passObstacles)continue
    seen+=n;queue+=n
   }
  }
  return false
 }

 private fun shortestPath(start:Cell,targets:Set<Cell>,cells:Map<Cell,CellScores>,forbidden:Set<Cell>):List<Action>?{
  val q=PriorityQueue<Node>();q+=Node(0,start,emptyList());val best=mutableMapOf(start to 0)
  while(q.isNotEmpty()){val n=q.remove();if(n.cost!=best[n.cell])continue;if(n.cell in targets&&n.path.isNotEmpty())return n.path
   for(d in Direction.entries){val next=Cell(n.cell.row+d.dr,n.cell.col+d.dc);if(next in forbidden)continue;val s=cells[next]?:continue;val obstacle=s.obstacle();val cost=n.cost+(if(obstacle)OBSTACLE_COST else 1)
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
 ):List<Action>{
  val first=choose(player,cells,history,dashAvailable,preview,forbiddenObstacles,dashCharges,stuck,claws)?:return emptyList()
  val single=listOf(first)
  if(maxSteps<=1||first.kind!=ActionKind.MOVE)return single
  val mayBreak=claws!=null&&claws>CLAW_RESERVE
  val blocked=forbiddenObstacles+(if(mayBreak)emptySet() else cells.filterValues{it.obstacle()}.keys)
   val itemRoute=itemPath(player,cells,blocked)
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

package de.robinthor.digiworldexplorer.strategy

import de.robinthor.digiworldexplorer.detection.Cell
import de.robinthor.digiworldexplorer.detection.CellScores
import java.util.PriorityQueue

enum class Direction(val dr:Int,val dc:Int){RIGHT(0,1),DOWN(1,0),UP(-1,0),LEFT(0,-1)}
enum class ActionKind{MOVE,ATTACK,DASH}
data class Action(val kind:ActionKind,val target:Cell,val direction:Direction,val reason:String)

object MovementPlanner{
 private data class Node(val cost:Int,val cell:Cell,val first:Action?):Comparable<Node>{override fun compareTo(o:Node)=cost.compareTo(o.cost)}
 fun choose(player:Cell,cells:Map<Cell,CellScores>,history:List<Cell>,dashAvailable:Boolean=false,preview:Map<Cell,CellScores> = emptyMap(),forbiddenObstacles:Set<Cell> = emptySet()):Action?{
  val items=cells.filter{(cell,s)->cell!=player&&s.item>.06}.keys
  if(items.isNotEmpty())shortest(player,items,cells,forbiddenObstacles)?.let{return it.copy(reason="item route")}
  var consecutive=0
  for(col in player.col+1..4){if(cells[Cell(player.row,col)]?.obstacle()==true)consecutive++ else break}
  if(dashAvailable&&consecutive>=2)return Action(ActionKind.DASH,player,Direction.RIGHT,"$consecutive right obstacles")
  val oscillating=history.size>=4&&history.takeLast(4).let{it[0]==it[2]&&it[1]==it[3]}
  val previous=history.dropLast(1).lastOrNull()
  return Direction.entries.mapNotNull{d->
   val n=Cell(player.row+d.dr,player.col+d.dc);val s=cells[n]?:return@mapNotNull null;if(n in forbiddenObstacles)return@mapNotNull null
   var score=when(d){Direction.RIGHT->100;Direction.DOWN->20;Direction.UP->15;Direction.LEFT->-40}+ (s.highlight*20).toInt()
   if(n==previous)score-=80;if(oscillating&&n==history[history.lastIndex-1])score-=200
   val look=preview[Cell(n.row,5)];if(look!=null&&look.pyramid>.17&&look.item<=.06)score-=35;if((look?.item?:0.0)>.06)score+=45
   score to Action(if(s.obstacle())ActionKind.ATTACK else ActionKind.MOVE,n,d,"explore right")
  }.maxByOrNull{it.first}?.second
 }
 private fun shortest(start:Cell,targets:Set<Cell>,cells:Map<Cell,CellScores>,forbidden:Set<Cell>):Action?{
  val q=PriorityQueue<Node>();q+=Node(0,start,null);val best=mutableMapOf(start to 0)
  while(q.isNotEmpty()){val n=q.remove();if(n.cost!=best[n.cell])continue;if(n.cell in targets&&n.first!=null)return n.first
   for(d in Direction.entries){val next=Cell(n.cell.row+d.dr,n.cell.col+d.dc);if(next in forbidden)continue;val s=cells[next]?:continue;val obstacle=s.obstacle();val cost=n.cost+(if(obstacle)2 else 1)
    if(cost<(best[next]?:999)){best[next]=cost;val first=n.first?:Action(if(obstacle)ActionKind.ATTACK else ActionKind.MOVE,next,d,"item route");q+=Node(cost,next,first)}}}
  return null
 }
}
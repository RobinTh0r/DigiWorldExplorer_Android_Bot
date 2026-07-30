package de.robinthor.digiworldexplorer.strategy

import de.robinthor.digiworldexplorer.detection.Cell

enum class Direction(val dr:Int,val dc:Int){ RIGHT(0,1),DOWN(1,0),UP(-1,0),LEFT(0,-1) }
data class Move(val target:Cell,val direction:Direction)

object MovementPlanner {
    fun choose(player:Cell, obstacles:Set<Cell>, history:List<Cell>):Move? {
        val candidates=Direction.entries.mapNotNull { direction ->
            val next=Cell(player.row+direction.dr,player.col+direction.dc)
            if(next.row !in 0..4 || next.col !in 0..4 || next in obstacles) null else Move(next,direction)
        }
        if(candidates.isEmpty()) return null
        val oscillating = history.size>=4 && history.takeLast(4).let { it[0]==it[2] && it[1]==it[3] }
        val previous = history.dropLast(1).lastOrNull()
        return candidates.maxByOrNull { move ->
            var score=when(move.direction){Direction.RIGHT->100;Direction.DOWN->20;Direction.UP->15;Direction.LEFT->-40}
            if(move.target==previous) score-=80
            if(oscillating && move.target==history[history.lastIndex-1]) score-=200
            score
        }
    }
}
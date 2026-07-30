package de.robinthor.digiworldexplorer.strategy

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.detection.Cell
import de.robinthor.digiworldexplorer.detection.CellScores
import de.robinthor.digiworldexplorer.detection.GridBounds

object AutoMoveController {
 private const val MIN_GRID=.82
 private const val MIN_PLAYER=.08
 private const val TAP_DELAY_MS=1200L
 private val main=Handler(Looper.getMainLooper())
 private val history=ArrayDeque<Cell>()
 private var candidate:Cell?=null;private var stable=0;private var lastTap=0L;private var tapPending=false

 fun onAnalysis(confidence:Double,bounds:GridBounds,cells:Map<Cell,CellScores>){
  if(confidence<MIN_GRID||tapPending)return
  val playerEntry=cells.maxByOrNull{it.value.player}?:return
  if(playerEntry.value.player<MIN_PLAYER)return
  val player=playerEntry.key
  if(candidate==player)stable++ else {candidate=player;stable=1}
  if(stable<2||SystemClock.elapsedRealtime()-lastTap<TAP_DELAY_MS)return
  if(history.lastOrNull()!=player){history.addLast(player);while(history.size>8)history.removeFirst()}
  val obstacles=cells.filter{(cell,score)->cell!=player&&score.obstacle()}.keys
  val move=MovementPlanner.choose(player,obstacles,history.toList())?:return
  val x=bounds.left+(move.target.col+.5f)*(bounds.right-bounds.left)/5f
  val y=bounds.top+(move.target.row+.5f)*(bounds.bottom-bounds.top)/5f
  val service=DigiWorldAccessibilityService.instance?:return
  tapPending=true;stable=0;lastTap=SystemClock.elapsedRealtime()
  main.post{service.dispatchValidatedTap(x,y){ok->tapPending=false;Log.i("DigiWorldAuto","tap=$ok player=$player target=${move.target} direction=${move.direction}")}}
 }
 fun reset(){candidate=null;stable=0;tapPending=false;history.clear()}
}
package de.robinthor.digiworldexplorer.strategy

import android.os.*
import android.util.Log
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.detection.*

object AutoMoveController{
 private const val MIN_GRID=.82;private const val MIN_PLAYER=.08;private const val TAP_DELAY=1200L
 private val main=Handler(Looper.getMainLooper());private val history=ArrayDeque<Cell>()
 private var candidate:Cell?=null;private var stable=0;private var lastTap=0L;private var pending=false
 fun onAnalysis(confidence:Double,bounds:GridBounds,cells:Map<Cell,CellScores>){
  val service=DigiWorldAccessibilityService.instance;val entry=cells.maxByOrNull{it.value.player};val player=entry?.key;val valid=confidence>=MIN_GRID&&entry!=null&&entry.value.player>=MIN_PLAYER
  val obstacles=cells.filter{(c,s)->c!=player&&s.obstacle()}.keys;val items=cells.filter{(c,s)->c!=player&&s.item>.06}.keys
  if(!valid){service?.updateOverlay(bounds,player,items,obstacles,null,"PAUSE: Erkennung unsicher",AutomationState.overlayEnabled);candidate=null;stable=0;return}
  if(history.lastOrNull()!=player){history.addLast(player!!);while(history.size>8)history.removeFirst()}
  val action=MovementPlanner.choose(player!!,cells,history.toList(),dashAvailable=false)
  val status=if(AutomationState.enabled)"AUTO: ${action?.kind?:"STOP"} ${action?.reason?:"keine Route"}" else "PAUSE: Automatik aus"
  service?.updateOverlay(bounds,player,items,obstacles,action?.target,status,AutomationState.overlayEnabled)
  if(!AutomationState.enabled||pending||action==null||action.kind==ActionKind.DASH)return
  if(candidate==player)stable++ else{candidate=player;stable=1}
  if(stable<2||SystemClock.elapsedRealtime()-lastTap<TAP_DELAY)return
  val x=bounds.left+(action.target.col+.5f)*(bounds.right-bounds.left)/5f;val y=bounds.top+(action.target.row+.5f)*(bounds.bottom-bounds.top)/5f
  if(service==null)return;pending=true;stable=0;lastTap=SystemClock.elapsedRealtime();main.post{service.dispatchValidatedTap(x,y){ok->pending=false;Log.i("DigiWorldAuto","tap=$ok kind=${action.kind} player=$player target=${action.target} direction=${action.direction}")}}
 }
 fun reset(){candidate=null;stable=0;pending=false;history.clear()}
}
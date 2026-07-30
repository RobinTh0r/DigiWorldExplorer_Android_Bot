package de.robinthor.digiworldexplorer.strategy
import android.os.*
import android.util.Log
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.detection.*
object AutoMoveController{
 private const val MIN_GRID=.82;private const val MIN_PLAYER=.08;private const val TAP_DELAY=500L
 private val main=Handler(Looper.getMainLooper());private val history=ArrayDeque<Cell>();private val recentItems=mutableMapOf<Cell,Int>()
 private var candidate:Cell?=null;private var stable=0;private var lastTap=0L;private var pending=false;private var previous:Cell?=null;private var expected:Cell?=null
 private val forbiddenObstacles=mutableSetOf<Cell>();private var lastAttackTarget:Cell?=null;private var lastAttackPlayer:Cell?=null;private var unchangedAttackFrames=0
 fun onAnalysis(confidence:Double,bounds:GridBounds,cells:Map<Cell,CellScores>,preview:Map<Cell,CellScores>){
  recentItems.replaceAll{_,ttl->ttl-1};recentItems.entries.removeIf{it.value<=0};cells.filter{it.value.item>.06}.keys.forEach{recentItems[it]=6}
  val service=DigiWorldAccessibilityService.instance;val entry=PlayerSelector.select(cells,previous,expected,recentItems.keys,MIN_PLAYER);val player=entry?.key;val valid=confidence>=MIN_GRID&&entry!=null
  val obstacles=cells.filter{(c,s)->c!=player&&s.obstacle()}.keys+preview.filter{it.value.pyramid>.17&&it.value.item<=.06}.keys;val items=cells.filter{(c,s)->c!=player&&s.item>.06}.keys+preview.filter{it.value.item>.06}.keys
  if(!valid){service?.updateOverlay(bounds,player,items,obstacles,null,"PAUSE: Spieler unsicher",AutomationState.overlayEnabled);candidate=null;stable=0;return}
  forbiddenObstacles.removeIf{cells[it]?.obstacle()!=true}
  lastAttackTarget?.let{target->if(player==lastAttackPlayer&&cells[target]?.obstacle()==true){unchangedAttackFrames++;if(unchangedAttackFrames>=2)forbiddenObstacles+=target}else{lastAttackTarget=null;lastAttackPlayer=null;unchangedAttackFrames=0}}
  if(expected==player)expected=null;previous=player
  if(history.lastOrNull()!=player){history.addLast(player!!);while(history.size>8)history.removeFirst()}
  val action=MovementPlanner.choose(player!!,cells,history.toList(),false,preview,forbiddenObstacles);val status=if(AutomationState.enabled)"AUTO: ${action?.kind?:"STOP"} ${action?.reason?:"keine Route"}" else "PAUSE: Automatik aus"
  service?.updateOverlay(bounds,player,items,obstacles,action?.target,status,AutomationState.overlayEnabled)
  if(!AutomationState.enabled||pending||action==null||action.kind==ActionKind.DASH)return
  if(candidate==player)stable++ else{candidate=player;stable=1};if(stable<2||SystemClock.elapsedRealtime()-lastTap<TAP_DELAY)return
  val x=bounds.left+(action.target.col+.5f)*(bounds.right-bounds.left)/5f;val y=bounds.top+(action.target.row+.5f)*(bounds.bottom-bounds.top)/5f;if(service==null)return
  pending=true;stable=0;lastTap=SystemClock.elapsedRealtime();if(action.kind==ActionKind.MOVE)expected=action.target else if(action.kind==ActionKind.ATTACK){lastAttackTarget=action.target;lastAttackPlayer=player;unchangedAttackFrames=0}
  main.post{service.dispatchValidatedTap(x,y){ok->pending=false;if(!ok)expected=null;Log.i("DigiWorldAuto","tap=$ok kind=${action.kind} player=$player target=${action.target} direction=${action.direction}")}}
 }
 fun reset(){candidate=null;stable=0;pending=false;history.clear();recentItems.clear();forbiddenObstacles.clear();lastAttackTarget=null;lastAttackPlayer=null;unchangedAttackFrames=0;previous=null;expected=null}
}
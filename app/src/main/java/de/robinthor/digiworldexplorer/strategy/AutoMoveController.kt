package de.robinthor.digiworldexplorer.strategy
import android.os.*
import android.util.Log
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.input.SafeTapRandomizer
import de.robinthor.digiworldexplorer.detection.*
object AutoMoveController{
 private const val MIN_GRID=.82;private const val MIN_PLAYER=.08
 /** Mindestabstand zwischen zwei Taps. 800 ms geben dem Spiel Zeit fuer Animation + Dialog
  *  und der Analyse ein sauber gerendertes Bild. */
 private const val TAP_DELAY=800L
 /** So viele Analysen ohne Positionswechsel gelten als festgefahren - erst dann ist ein Dash erlaubt. */
 private const val STUCK_FRAMES=10
 /** HUD-Ziffern sind auf manchen Aufloesungen nicht sicher lesbar. Drei ist der begrenzte
  * Fallback fuer den bestaetigten Testvorrat; drei Fehlversuche sperren weitere Dashes. */
 private const val UNKNOWN_DASH_FALLBACK=3
 private const val UNKNOWN_CLAW_FALLBACK=4
 /** So viele Analysen ohne echten Rechts-Fortschritt (Brett scrollt nicht) gelten ebenfalls als
  *  festgefahren. Reines Stillstehen (STUCK_FRAMES) erkennt keine Einkesselung, in der die Figur
  *  zwischen wenigen Zellen hin- und herlaeuft, ohne je auf derselben Zelle zu verharren - dann
  *  wurde nie gedasht, obwohl kein Weg herausfuehrte. */
 private const val NO_PROGRESS_LIMIT=25
 /** Nach fuenf wirklich ausgefuehrten Aktionen ohne Brett- oder Rechtsfortschritt wird komplett
  *  gestoppt. Das verhindert endlose 2-3-Zellen-Schleifen und spart Schritte. */
 private const val MAX_FAILED_ACTIONS=5
 /** So viele Analysen ohne erkannten Spieler, bis die Verfolgung komplett neu aufgesetzt wird. */
 private const val LOST_LIMIT=15
 /** So viele ruhige Analysen darf ein erwartetes Zugziel unerreicht bleiben, bevor es verworfen
  *  wird. Beim Laufen nach rechts scrollt das Brett mit, die Figur bleibt dann auf derselben
  *  Bildschirmzelle - das erwartete Ziel wird in diesem Fall nie erreicht. */
 private const val EXPECTED_TTL=4
 /** Zwei Bilder gelten als gleich, wenn sich kein Zellwert um mehr als diesen Betrag unterscheidet.
  *  Vorher wurde auf exakte Gleichheit gerundeter Werte geprueft - lag ein Score genau auf einer
  *  Rundungsgrenze, kippte er dauerhaft zwischen zwei Stufen und das Bild galt nie als ruhig. */
 private const val SETTLE_TOLERANCE=.02
 /** Notbremse, falls das Bild wegen dauerhafter Animation nie ruhig wird. */
 private const val SETTLE_LIMIT=12
 /** Bleibt die Tap-Rueckmeldung aus, haengt die Automatik sonst fuer immer. */
 private const val PENDING_TIMEOUT=2500L
 /** So viele Schritte werden am Stueck getippt, wenn ein Collectable das Ziel ist. */
 private const val BURST_ITEM=2
 /** Ohne Collectable zaehlt nur Strecke - dann laufen wir gleich so weit am Stueck. */
 private const val BURST_RIGHT=3
 /** Abstand der Taps innerhalb eines Buendels. Muss die Laufanimation abdecken. */
 /** Abstand zwischen Burst-Taps - gleich wie TAP_DELAY, damit jeder Tap
  *  im selben 0,8-s-Rhythmus landet und die Analyse dazwischen sauber lesen kann. */
 private const val BURST_DELAY=800L
 /** Ab diesem Schriftanteil gilt eine Zelle als von einer Meldung ueberdeckt. */
 private const val DIALOG_TEXT=.08
 /** So viele ueberdeckte Zellen gelten als Dialog - eine Meldung zieht sich ueber das ganze Brett. */
 private const val DIALOG_CELLS=3
 private const val BLIND_STAGE_INTERVAL=10_000L
 private const val DWS_CONTEXT_TTL=8_000L
 private val main=Handler(Looper.getMainLooper());private val history=ArrayDeque<Cell>();private val recentItems=mutableMapOf<Cell,Int>()
 private var candidate:Cell?=null;private var stable=0;private var lastTap=0L;private var nextTapDelay=TAP_DELAY;private var pending=false;private var previous:Cell?=null;private var expected:Cell?=null
 /** Wird gesetzt, sobald onAnalysis eine Meldung im Bild erkennt. dispatchBurst liest diesen
  *  Wert auf dem Main-Thread, daher volatile. Ein laufendes Burst-Buendel bricht ab, sobald
  *  die Analyse einen Dialog meldet - ohne diese Pruefung landen die nachfolgenden Taps blind
  *  in der gerade angezeigten Fehlermeldung des Spiels. */
 @Volatile private var dialogActive=false
 private val forbiddenObstacles=mutableSetOf<Cell>();private var lastAttackTarget:Cell?=null;private var lastAttackPlayer:Cell?=null;private var unchangedAttackFrames=0
 private var sameCellFrames=0;private var furthestCol=-1;private var trackingConfirmed=false;private var lostFrames=0;private var noProgressFrames=0;private var dashFailures=0;private var actionsWithoutProgress=0
 /** Ressourcen werden nicht blind wiederholt: Bleibt nach Angriff/Dash Position und Brett
  *  zweimal unveraendert, gilt die Aktion fuer diese Sitzung als leer oder unwirksam. */
 private var lastResourceKind:ActionKind?=null;private var resourcePlayer:Cell?=null;private var resourceUnchangedFrames=0
 private var attackUnavailable=false;private var dashUnavailable=false;private var dashBlockedUntil=0L
 private var lastDwsGridSeen=0L;private var lastBlindStageTap=0L
 private var lastSignature:List<Double> = emptyList();private var lastSettledSignature:List<Double> = emptyList();private var expectedAge=0;private var unsettledFrames=0
 /** Laeuft die Figur nach rechts, scrollt bei diesem Spiel das Brett mit und die Figur bleibt auf
  *  derselben Bildschirmzelle. Das wird nicht angenommen, sondern am ersten Einzelschritt gemessen -
  *  ohne dieses Wissen laesst sich kein zweiter Tap im Voraus berechnen, also wird auch nicht
  *  gebuendelt, solange der Wert `null` ist. */
 private var rightScrolls:Boolean?=null;private var probeFrom:Cell?=null;private var expectedRight:Cell?=null
 fun onAnalysis(confidence:Double,bounds:GridBounds,cells:Map<Cell,CellScores>,preview:Map<Cell,CellScores>,dashButton:Pair<Float,Float>?=null,hud:HudCounters=HudCounters()){
  // Die Zelle, auf der die Figur steht (bzw. gleich stehen wird), darf nie als Item gesperrt
  // werden - ihr eigenes Sprite haelt den Item-Score sonst dauerhaft oben und die Figur
  // wird unauffindbar.
  val self=setOfNotNull(previous,expected)
  recentItems.replaceAll{_,ttl->ttl-1};recentItems.entries.removeIf{it.value<=0||it.key in self}
  cells.filter{(_,score)->score.item>.06&&(!AutomationState.dwsNavigationSettings.collectOnlyEnergy||score.orange>.06)}
   .filterKeys{it !in self}.keys.forEach{recentItems[it]=6}
  val service=DigiWorldAccessibilityService.instance
  if(pending&&SystemClock.elapsedRealtime()-lastTap>PENDING_TIMEOUT){
   Log.w("DigiWorldAuto","Tap-Rueckmeldung ausgeblieben - Sperre aufgehoben");pending=false;expected=null;trackingConfirmed=false
  }
  val entry=PlayerSelector.select(cells,previous,expected,recentItems.keys,MIN_PLAYER);val player=entry?.key;val valid=confidence>=MIN_GRID&&entry!=null
  if(valid)lastDwsGridSeen=SystemClock.elapsedRealtime()
  val obstacles=cells.filter{(c,s)->c!=player&&s.obstacle()}.keys+preview.filter{it.value.pyramid>.17&&it.value.item<=.06}.keys
  val onlyEnergy=AutomationState.dwsNavigationSettings.collectOnlyEnergy
  val energyVisible=cells.any{(c,s)->c!=player&&s.orange>.06}
  val items=cells.filter{(c,s)->c!=player&&s.item>.06&&(!onlyEnergy||s.orange>.06)}.keys+
   preview.filter{it.value.item>.06&&(!onlyEnergy||it.value.orange>.06)}.keys
  // Legt das Spiel eine Meldung ueber das Brett ("Bewegung nicht moeglich"), sind saemtliche
  // Zellwerte wertlos. Wird hier weitergetippt, verschiebt sich die Verfolgung endgueltig.
  val texty=cells.count{it.value.text>DIALOG_TEXT}
  if(texty>=DIALOG_CELLS){
   val now=SystemClock.elapsedRealtime()
   if(AutomationState.dwsNavigationSettings.blindStageFailedTap&&now-lastDwsGridSeen<=DWS_CONTEXT_TTL&&now-lastBlindStageTap>=BLIND_STAGE_INTERVAL&&service!=null){
    lastBlindStageTap=now;dispatchBlindStageDismiss(service,bounds)
   }
   Log.i("DigiWorldAuto","Meldung im Bild ($texty Zellen mit Schrift) - Automatik wartet")
   service?.updateOverlay(bounds,null,emptySet(),emptySet(),null,service.getString(R.string.overlay_wait_dialog),AutomationState.overlayEnabled,hud,dashButton)
   dialogActive=true;candidate=null;stable=0;expected=null;expectedAge=0;trackingConfirmed=false;probeFrom=null;return
  }
  dialogActive=false
  if(!valid){
   lostFrames++
   if(lostFrames>=LOST_LIMIT){
    Log.w("DigiWorldAuto","Verfolgung nach $lostFrames Analysen ohne Spieler zurueckgesetzt")
    recentItems.clear();expected=null;previous=null;trackingConfirmed=false;lostFrames=0
   }
   val reason=when{cells.isEmpty()->"kein Spielbild";confidence<MIN_GRID->"Raster unsicher %.2f".format(confidence);else->"Spieler unsicher (%d Kandidaten, best %.3f)".format(cells.count{it.value.player>=MIN_PLAYER},cells.values.maxOf{it.player})}
   Log.i("DigiWorldAuto","pause reason=$reason previous=$previous expected=$expected recentItems=${recentItems.keys}")
   val overlayStatus=when{cells.isEmpty()->service?.getString(R.string.overlay_no_game)?:reason;confidence<MIN_GRID->service?.getString(R.string.overlay_grid_uncertain)?:reason;else->service?.getString(R.string.overlay_player_uncertain)?:reason}
   // Ein einzelner unsicherer Analyseframe darf das zuletzt stabile Raster nicht loeschen.
   // ScreenCaptureService entfernt es weiterhin nach drei Sekunden ohne erkannten Spielinhalt.
   if(cells.isEmpty()||confidence<MIN_GRID)service?.updateStatusKeepingGrid(overlayStatus,AutomationState.overlayEnabled)
   else service?.updateOverlay(bounds,player,items,obstacles,null,overlayStatus,AutomationState.overlayEnabled,hud,dashButton)
   candidate=null;stable=0;return
  }
  lostFrames=0
  // Waehrend eines Zuges scrollt das Brett. In diesen Zwischenbildern liegen Sprites und
  // Markierungen zwischen zwei Zellen und werden voellig falsch klassifiziert. Gehandelt wird
  // deshalb nur, wenn zwei aufeinanderfolgende Analysen dasselbe Brett zeigen.
  val signature=(0..4).flatMap{r->(0..4).map{c->cells[Cell(r,c)]?.pyramid?:0.0}}
  val quiet=lastSignature.size==signature.size&&signature.indices.all{kotlin.math.abs(signature[it]-lastSignature[it])<=SETTLE_TOLERANCE}
  lastSignature=signature
  if(quiet)unsettledFrames=0 else unsettledFrames++
  val settled=quiet||unsettledFrames>=SETTLE_LIMIT
  if(settled&&!quiet)Log.w("DigiWorldAuto","Bild nach $unsettledFrames Analysen nie ruhig - handle trotzdem")
  if(!settled){
   service?.updateOverlay(bounds,player,items,obstacles,null,service.getString(R.string.overlay_wait_motion),AutomationState.overlayEnabled,hud,dashButton)
   candidate=null;stable=0;return
  }
  unsettledFrames=0
  // Ein veraendertes Brett bei ruhigem Bild heisst: die Welt ist weitergescrollt, also Fortschritt.
  val advanced=lastSettledSignature.isNotEmpty()&&signature.indices.any{kotlin.math.abs(signature[it]-lastSettledSignature[it])>SETTLE_TOLERANCE*2}
  lastSettledSignature=signature
  forbiddenObstacles.removeIf{cells[it]?.obstacle()!=true}
  lastAttackTarget?.let{target->if(player==lastAttackPlayer&&cells[target]?.obstacle()==true){unchangedAttackFrames++;if(unchangedAttackFrames>=2)forbiddenObstacles+=target}else{lastAttackTarget=null;lastAttackPlayer=null;unchangedAttackFrames=0}}
  if(expected==player){expected=null;expectedAge=0;trackingConfirmed=true}
  else if(expected!=null){expectedAge++;if(expectedAge>=EXPECTED_TTL){expected=null;expectedAge=0}}
  // Messung des Scrollverhaltens am Einzelschritt nach rechts.
  probeFrom?.let{from->
   if(player==expectedRight){rightScrolls=false;probeFrom=null;Log.i("DigiWorldAuto","Rechtsschritt: Figur laeuft, Brett steht")}
   else if(advanced&&player==from){rightScrolls=true;probeFrom=null;Log.i("DigiWorldAuto","Rechtsschritt: Brett scrollt, Figur bleibt stehen")}
  }
  val playerBefore=previous
  previous=player
  if(history.lastOrNull()!=player){history.addLast(player!!);while(history.size>8)history.removeFirst();sameCellFrames=0} else sameCellFrames++
  if(advanced)sameCellFrames=0
  // advanced=true heisst: das Brett ist wirklich weitergescrollt, also echter Fortschritt -
  // unabhaengig davon, ob die Figur dabei staendig die Zelle wechselt (Einkesselung sieht wie
  // Bewegung aus, bringt aber nie ein neues Brettbild).
  // advanced=true heisst: das Brett ist wirklich weitergescrollt, also echter Fortschritt -
  // unabhaengig davon, ob die Figur dabei staendig die Zelle wechselt (Einkesselung sieht wie
  // Bewegung aus, bringt aber nie ein neues Brettbild).
  // Links/rechts zwischen denselben zwei Feldern ist kein Fortschritt. Ein Feld zaehlt nur beim
  // erstmaligen Erreichen einer weiter rechts liegenden Spalte oder wenn das Brett wirklich scrollt.
  val newRightmost=player!!.col>furthestCol
  val progressed=advanced||newRightmost
  if(advanced)furthestCol=player.col else if(newRightmost)furthestCol=player.col
  if(progressed){
   // Nach einem erfolgreichen Angriff erst durch die geoeffnete Luecke laufen; das Folgebild
   // kann noch Wandanimation enthalten und darf keinen unnoetigen Dash ausloesen.
   if(lastResourceKind==ActionKind.ATTACK)dashBlockedUntil=SystemClock.elapsedRealtime()+2_500L
   noProgressFrames=0;dashFailures=0;actionsWithoutProgress=0;attackUnavailable=false;dashUnavailable=false
  } else noProgressFrames++
  lastResourceKind?.let{kind->
   if(progressed||player!=resourcePlayer){
    Log.i("DigiWorldAuto","$kind brachte Fortschritt")
    lastResourceKind=null;resourcePlayer=null;resourceUnchangedFrames=0
   }else{
    resourceUnchangedFrames++
    if(resourceUnchangedFrames>=2){
     if(kind==ActionKind.ATTACK)attackUnavailable=true else if(kind==ActionKind.DASH)dashUnavailable=true
     Log.w("DigiWorldAuto","$kind brachte keinen Fortschritt - versuche Alternative")
     lastResourceKind=null;resourcePlayer=null;resourceUnchangedFrames=0
     if(attackUnavailable&&dashUnavailable){
      Log.w("DigiWorldAuto","Notstopp: Angriff und Dash ohne Fortschritt")
      AutomationState.enabled=false;AutomationState.overlayEnabled=false;service?.setOverlayEnabled(false)
      if(service!=null)main.post{ScreenCaptureService.stopForStuck(service)}
      return
     }
    }
   }
  }
  val looping=history.size>=4&&history.takeLast(4).let{it[0]==it[2]&&it[1]==it[3]}
  val stuck=AutomationState.enabled&&(looping||actionsWithoutProgress>=3||sameCellFrames>=STUCK_FRAMES||noProgressFrames>=NO_PROGRESS_LIMIT)
  if(AutomationState.enabled&&actionsWithoutProgress>=MAX_FAILED_ACTIONS){
   Log.w("DigiWorldAuto","Stopp nach $actionsWithoutProgress Aktionen ohne Fortschritt")
   AutomationState.enabled=false;AutomationState.overlayEnabled=false;service?.setOverlayEnabled(false)
   if(service!=null)main.post{ScreenCaptureService.stopForStuck(service)}
   return
  }
  // Gebuendelt wird nur, wenn die Verfolgung sitzt und das Scrollverhalten gemessen ist.
  val burst=if(!trackingConfirmed||rightScrolls==null||(AutomationState.dwsNavigationSettings.betterEnergyCollect&&energyVisible))1
   else if(cells.any{(c,s)->c!=player&&s.item>.06&&(!onlyEnergy||s.orange>.06)})BURST_ITEM else BURST_RIGHT
  // Bringt ein Dash nach mehreren Versuchen nie echten Fortschritt (0 Ladungen oder Knopf falsch
  // erkannt), wuerde er sonst jede Analyse erneut vorgeschlagen und die Automatik haengt fest.
  val plan=MovementPlanner.plan(player!!,cells,history.toList(),dashAvailable=dashButton!=null&&dashFailures<3&&!dashUnavailable&&SystemClock.elapsedRealtime()>=dashBlockedUntil,preview=preview,forbiddenObstacles=forbiddenObstacles,dashCharges=if(dashUnavailable)0 else hud.dash?:UNKNOWN_DASH_FALLBACK,stuck=stuck,claws=if(attackUnavailable)0 else hud.claws?:UNKNOWN_CLAW_FALLBACK,maxSteps=burst,settings=AutomationState.dwsNavigationSettings)
  val action=plan.firstOrNull()
  val actionLabel=when(action?.kind){ActionKind.MOVE->service?.getString(R.string.overlay_action_move);ActionKind.ATTACK->service?.getString(R.string.overlay_action_attack);ActionKind.DASH->service?.getString(R.string.overlay_action_dash);null->service?.getString(R.string.overlay_action_stop)};val status=if(AutomationState.enabled)service?.getString(R.string.overlay_auto_action,actionLabel?:"")+(if(plan.size>1)" x${plan.size}" else "") else service?.getString(R.string.overlay_paused).orEmpty()
  service?.updateOverlay(bounds,player,items,obstacles,action?.target,status,AutomationState.overlayEnabled,hud,dashButton)
  if(!AutomationState.enabled||pending||action==null)return
  // Nach einem bestaetigten Zug reicht eine Analyse zur Bestaetigung, sonst zwei.
  val needed=if(trackingConfirmed)1 else 2
  if(candidate==player)stable++ else{candidate=player;stable=1};if(stable<needed||SystemClock.elapsedRealtime()-lastTap<nextTapDelay)return
  if(service==null)return
  val taps=mutableListOf<Pair<Float,Float>>()
  if(action.kind==ActionKind.DASH){
   val button=dashButton?:return;taps+=button
  } else {
   // Bei scrollendem Brett wandert jedes geplante Feld pro bereits ausgefuehrtem Rechtsschritt
   // eine Spalte nach links, die Figur bleibt stehen. Sonst laeuft die Figur einfach mit.
   var pr=player.row;var pc=player.col;var shift=0
   for(step in plan){
    val tr=step.target.row;val tc=step.target.col-shift
    if(tc<0||tc>4)break
    taps+=cellCenter(bounds,tr,tc)
    if(step.direction==Direction.RIGHT&&rightScrolls==true)shift++ else{pr=tr;pc=tc}
   }
   if(taps.isEmpty())return
   expected=if(taps.size>1)Cell(pr,pc) else action.target
   expectedAge=0
  }
  pending=true;stable=0;lastTap=SystemClock.elapsedRealtime()
  actionsWithoutProgress+=taps.size
  when(action.kind){
   ActionKind.MOVE->{
    // Solange das Scrollverhalten unbekannt ist, dient der Einzelschritt als Messung.
    if(taps.size==1&&action.direction==Direction.RIGHT&&rightScrolls==null){probeFrom=player;expectedRight=action.target}
   }
   ActionKind.ATTACK->{expected=null;lastAttackTarget=action.target;lastAttackPlayer=player;unchangedAttackFrames=0;lastResourceKind=ActionKind.ATTACK;resourcePlayer=player;resourceUnchangedFrames=0}
   ActionKind.DASH->{expected=null;expectedAge=0;sameCellFrames=0;dashFailures++;lastResourceKind=ActionKind.DASH;resourcePlayer=player;resourceUnchangedFrames=0}
  }
  val info="kind=${action.kind} player=$player target=${action.target} direction=${action.direction} reason=${action.reason}"
  main.post{dispatchBurst(service,taps,0,info)}
 }

 private fun dispatchBlindStageDismiss(service:DigiWorldAccessibilityService,bounds:GridBounds){
  val gridWidth=(bounds.right-bounds.left).toFloat();val gridHeight=(bounds.bottom-bounds.top).toFloat()
  val radius=minOf(gridWidth*.08f,service.resources.displayMetrics.density*38f)
  val baseX=bounds.left+gridWidth*.50f;val baseY=bounds.bottom+gridHeight*.33f
  val taps=if(kotlin.random.Random.nextBoolean())2 else 1
  fun tap(index:Int){
   val (x,y)=SafeTapRandomizer.point(baseX,baseY,radius,radius*.45f)
   service.dispatchValidatedTap(x,y){ok->
    Log.i("DigiWorldAuto","Stage-Failed-Testtap ${index+1}/$taps ok=$ok x=$x y=$y")
    if(ok&&index+1<taps)main.postDelayed({tap(index+1)},SafeTapRandomizer.delay(240L,80L))
   }
  }
  main.post{tap(0)}
 }
 private fun cellCenter(bounds:GridBounds,row:Int,col:Int):Pair<Float,Float> {
  val cw=(bounds.right-bounds.left)/5f;val ch=(bounds.bottom-bounds.top)/5f
  // Nur die inneren 16 % der Zelle werden genutzt: selbst der maximale Zufallswert bleibt
  // mehr als vier Zehntel einer Zellbreite von jedem Nachbarfeld entfernt.
  return SafeTapRandomizer.point(bounds.left+(col+.5f)*cw,bounds.top+(row+.5f)*ch,cw*.08f,ch*.08f)
 }

 private fun dispatchBurst(service:DigiWorldAccessibilityService,taps:List<Pair<Float,Float>>,index:Int,info:String){
  val (x,y)=taps[index]
  service.dispatchValidatedTap(x,y){ok->
   Log.i("DigiWorldAuto","tap=$ok schritt=${index+1}/${taps.size} $info")
   if(!ok||index+1>=taps.size){
    lastTap=SystemClock.elapsedRealtime();nextTapDelay=SafeTapRandomizer.delay(TAP_DELAY,60L);pending=false
    if(!ok){expected=null;trackingConfirmed=false;probeFrom=null}
   } else main.postDelayed({
    if(dialogActive){Log.i("DigiWorldAuto","Burst abgebrochen: Meldung im Bild");lastTap=SystemClock.elapsedRealtime();nextTapDelay=SafeTapRandomizer.delay(TAP_DELAY,60L);pending=false}
    else dispatchBurst(service,taps,index+1,info)
   },SafeTapRandomizer.delay(BURST_DELAY,60L))
  }
 }
 fun pauseForPurchaseScreen(){dialogActive=true;pending=false;expected=null;expectedAge=0;candidate=null;stable=0;probeFrom=null}
 fun reset(){candidate=null;stable=0;pending=false;nextTapDelay=TAP_DELAY;history.clear();recentItems.clear();forbiddenObstacles.clear();lastAttackTarget=null;lastAttackPlayer=null;unchangedAttackFrames=0;previous=null;expected=null;sameCellFrames=0;furthestCol=-1;trackingConfirmed=false;lostFrames=0;lastSignature=emptyList();lastSettledSignature=emptyList();expectedAge=0;unsettledFrames=0;probeFrom=null;expectedRight=null;noProgressFrames=0;dashFailures=0;actionsWithoutProgress=0;lastResourceKind=null;resourcePlayer=null;resourceUnchangedFrames=0;attackUnavailable=false;dashUnavailable=false;dashBlockedUntil=0L;lastDwsGridSeen=0L;lastBlindStageTap=0L}
}
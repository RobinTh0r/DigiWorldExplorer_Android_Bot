package de.robinthor.digiworldexplorer.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.provider.Settings
import de.robinthor.digiworldexplorer.detection.Cell
import de.robinthor.digiworldexplorer.detection.GridBounds
import de.robinthor.digiworldexplorer.detection.HudBox
import de.robinthor.digiworldexplorer.detection.HudCounterReader
import de.robinthor.digiworldexplorer.detection.HudCounters

class DigiWorldAccessibilityService:AccessibilityService(){
 private var overlay:GridOverlayView?=null
 override fun onServiceConnected(){instance=this;showOverlay()}
 override fun onAccessibilityEvent(event:AccessibilityEvent?)=Unit
 override fun onInterrupt()=Unit
 override fun onDestroy(){removeOverlay();if(instance===this)instance=null;super.onDestroy()}
 fun dispatchValidatedTap(x:Float,y:Float,onComplete:(Boolean)->Unit){if(x<0||y<0){onComplete(false);return};val p=Path().apply{moveTo(x,y)};val g=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,80)).build();val ok=dispatchGesture(g,object:GestureResultCallback(){override fun onCompleted(d:GestureDescription?)=onComplete(true);override fun onCancelled(d:GestureDescription?)=onComplete(false)},null);if(!ok)onComplete(false)}
 fun clearCalibrationOverlay(){overlay?.post{overlay?.bounds=null;overlay?.visibility=View.GONE;overlay?.invalidate()}}
 fun setOverlayEnabled(enabled:Boolean){overlay?.post{overlay?.visibility=if(enabled)View.VISIBLE else View.GONE}}
 fun hideForCapture(){overlay?.post{overlay?.captureMode=true;overlay?.invalidate()}}
 fun updateOverlay(bounds:GridBounds?,player:Cell?,items:Set<Cell>,obstacles:Set<Cell>,target:Cell?,status:String,visible:Boolean,hud:HudCounters=HudCounters()){overlay?.post{overlay?.apply{this.bounds=bounds;this.player=player;this.items=items;this.obstacles=obstacles;this.target=target;this.status=status;this.hud=hud;captureMode=false;visibility=if(visible)View.VISIBLE else View.GONE;invalidate()}}}
 // FLAG_LAYOUT_NO_LIMITS und LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS sind noetig, damit das Overlay im
 // selben Koordinatensystem liegt wie der Vollbild-Capture. Ohne beides ist das Fenster um die
 // Statusleistenhoehe nach unten versetzt; die gezeichneten Linien landen dann in den Abtastfenstern
 // der Zellen und verfaelschen die naechste Analyse (messbar: unterste Reihe komplett als Item erkannt).
 // FLAG_SECURE ist hier bewusst NICHT gesetzt: es schwaerzt auf Android 15 die gesamte MediaProjection.
 private fun showOverlay(){if(overlay!=null)return;android.util.Log.i("DigiWorldOverlay","create canDrawOverlays=${Settings.canDrawOverlays(this)}");overlay=GridOverlayView().also{getSystemService(WindowManager::class.java).addView(it,WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,if(Settings.canDrawOverlays(this)) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS})}}
 private fun removeOverlay(){overlay?.let{runCatching{getSystemService(WindowManager::class.java).removeView(it)}};overlay=null}
 inner class GridOverlayView:View(this){var bounds:GridBounds?=null;var player:Cell?=null;var items:Set<Cell> = emptySet();var obstacles:Set<Cell> = emptySet();var target:Cell?=null;var status="Analyse";var hud:HudCounters=HudCounters();var captureMode=false;private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE}
  init{background=ColorDrawable(Color.TRANSPARENT)}
  // Alle Maße sind relativ zur Zellgröße, damit nichts in die Abtastfenster der Klassifizierung ragt:
  // CellClassifier und PreviewClassifier lassen an jeder Zellkante 7% Rand aus. Linien liegen auf den
  // Zellgrenzen, Boxen bei 3,5% Einrückung, jeweils inklusive halber Strichstärke unter 6%. Dadurch
  // darf das Overlay dauerhaft sichtbar bleiben und muss für die Analyse nicht mehr ausgeblendet werden.
  override fun onDraw(c:Canvas){super.onDraw(c);val b=bounds?:return;if(captureMode)return;val cw=(b.right-b.left)/5f;val ch=(b.bottom-b.top)/5f;val unit=minOf(cw,ch)
   p.pathEffect=null;p.style=Paint.Style.STROKE;p.color=Color.GREEN;p.strokeWidth=unit*.025f;for(i in 0..5){c.drawLine(b.left+i*cw,b.top.toFloat(),b.left+i*cw,b.bottom.toFloat(),p);c.drawLine(b.left.toFloat(),b.top+i*ch,b.right.toFloat(),b.top+i*ch,p)}
   p.pathEffect=DashPathEffect(floatArrayOf(unit*.07f,unit*.05f),0f);c.drawRect(b.right.toFloat(),b.top.toFloat(),b.right+cw,b.bottom.toFloat(),p);for(i in 1..4)c.drawLine(b.right.toFloat(),b.top+i*ch,b.right+cw,b.top+i*ch,p);p.pathEffect=null
   val inset=unit*.035f
   fun box(cell:Cell,color:Int,w:Float){p.color=color;p.strokeWidth=w;val l=b.left+cell.col*cw+inset;val t=b.top+cell.row*ch+inset;c.drawRect(l,t,l+cw-2*inset,t+ch-2*inset,p)}
   obstacles.forEach{box(it,Color.RED,unit*.035f)};items.forEach{box(it,Color.MAGENTA,unit*.04f)};player?.let{box(it,Color.YELLOW,unit*.05f)};target?.let{box(it,Color.CYAN,unit*.05f)}
   // Status direkt unter dem Raster im hellen Bereich. Heller Umriss plus dunkle Füllung bleibt auf
   // hellem wie dunklem Untergrund lesbar; unterhalb von bounds.bottom liegt kein Abtastfenster mehr.
   val ts=ch*.24f;p.textSize=ts;val below=b.bottom+ts*1.15f;val ty=if(below<=height-ts*.3f)below else (b.top-ts*.45f).coerceAtLeast(ts)
   p.style=Paint.Style.STROKE;p.strokeWidth=ts*.20f;p.color=Color.WHITE;c.drawText(status,b.left.toFloat(),ty,p)
   p.style=Paint.Style.FILL;p.color=Color.rgb(12,20,36);c.drawText(status,b.left.toFloat(),ty,p)
   // Vorratszahlen einkasten. Das Overlay liegt im eigenen Kamerabild, deshalb sind hier zwei
   // Farben tabu: nahezu Weiss wuerde der Ziffernleser als Kontur zaehlen, und Cyan erfuellt genau
   // das Farbkriterium der Dash-Button-Suche (b>195, g>140, r<140) - beides mit rotem Anteil 255
   // ausgeschlossen. Die Beschriftung steht ausserdem rechts neben dem Abtastfenster der Ziffern.
   // Ein nicht sicher gelesener Wert wird als "?" gezeigt statt geraten zu werden.
   val labelX=b.left+(HudCounterReader.X_TO+.03).toFloat()*(b.right-b.left)
   fun counter(box:HudBox?,value:Int?,label:String){
    if(box==null)return
    val pad=ts*.18f
    p.style=Paint.Style.STROKE;p.strokeWidth=ts*.10f;p.color=if(value==null)Color.rgb(255,140,0) else Color.MAGENTA
    c.drawRect(box.left-pad,box.top-pad,box.right+pad,box.bottom+pad,p)
    val text="$label ${value?:"?"}";val tby=box.bottom.toFloat()
    p.textSize=ts*.7f;p.style=Paint.Style.STROKE;p.strokeWidth=ts*.16f;p.color=Color.WHITE;c.drawText(text,labelX,tby,p)
    p.style=Paint.Style.FILL;p.color=Color.rgb(12,20,36);c.drawText(text,labelX,tby,p)
    p.textSize=ts
   }
   counter(hud.clawsBox,hud.claws,"Krallen");counter(hud.dashBox,hud.dash,"Dash")}
 }
 companion object{@Volatile var instance:DigiWorldAccessibilityService?=null;private set}
}
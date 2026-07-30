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

class DigiWorldAccessibilityService:AccessibilityService(){
 private var overlay:GridOverlayView?=null
 override fun onServiceConnected(){instance=this;showOverlay()}
 override fun onAccessibilityEvent(event:AccessibilityEvent?)=Unit
 override fun onInterrupt()=Unit
 override fun onDestroy(){removeOverlay();if(instance===this)instance=null;super.onDestroy()}
 fun dispatchValidatedTap(x:Float,y:Float,onComplete:(Boolean)->Unit){if(x<0||y<0){onComplete(false);return};val p=Path().apply{moveTo(x,y)};val g=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,80)).build();val ok=dispatchGesture(g,object:GestureResultCallback(){override fun onCompleted(d:GestureDescription?)=onComplete(true);override fun onCancelled(d:GestureDescription?)=onComplete(false)},null);if(!ok)onComplete(false)}
 fun updateOverlay(bounds:GridBounds?,player:Cell?,items:Set<Cell>,obstacles:Set<Cell>,target:Cell?,status:String,visible:Boolean){overlay?.apply{this.bounds=bounds;this.player=player;this.items=items;this.obstacles=obstacles;this.target=target;this.status=status;visibility=if(visible)View.VISIBLE else View.GONE;invalidate()}}
 private fun showOverlay(){if(overlay!=null)return;android.util.Log.i("DigiWorldOverlay","create canDrawOverlays=${Settings.canDrawOverlays(this)}");overlay=GridOverlayView().also{getSystemService(WindowManager::class.java).addView(it,WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,if(Settings.canDrawOverlays(this)) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START})}}
 private fun removeOverlay(){overlay?.let{runCatching{getSystemService(WindowManager::class.java).removeView(it)}};overlay=null}
 inner class GridOverlayView:View(this){var bounds:GridBounds?=null;var player:Cell?=null;var items:Set<Cell> = emptySet();var obstacles:Set<Cell> = emptySet();var target:Cell?=null;var status="Analyse";private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=4f;textSize=30f}
  init{background=ColorDrawable(Color.TRANSPARENT)}
  override fun onDraw(c:Canvas){super.onDraw(c);val b=bounds?:return;val cw=(b.right-b.left)/5f;val ch=(b.bottom-b.top)/5f;p.color=Color.GREEN;p.style=Paint.Style.STROKE;for(i in 0..5){c.drawLine(b.left+i*cw,b.top.toFloat(),b.left+i*cw,b.bottom.toFloat(),p);c.drawLine(b.left.toFloat(),b.top+i*ch,b.right.toFloat(),b.top+i*ch,p)}
   fun box(cell:Cell,color:Int,w:Float){p.color=color;p.strokeWidth=w;val l=b.left+cell.col*cw+5;val t=b.top+cell.row*ch+5;c.drawRect(l,t,l+cw-10,t+ch-10,p)}
   obstacles.forEach{box(it,Color.RED,4f)};items.forEach{box(it,Color.MAGENTA,5f)};player?.let{box(it,Color.YELLOW,7f)};target?.let{box(it,Color.CYAN,7f)};p.style=Paint.Style.FILL;p.color=Color.GREEN;p.textSize=28f;c.drawText(status,b.left.toFloat(),(b.top-18).coerceAtLeast(32).toFloat(),p)} }
 companion object{@Volatile var instance:DigiWorldAccessibilityService?=null;private set}
}
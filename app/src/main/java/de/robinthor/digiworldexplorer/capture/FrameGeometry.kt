package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.vision.PixelFrame
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

data class PixelSize(val width:Int,val height:Int) {
    init { require(width>0 && height>0) }
}
data class PixelRect(val left:Int,val top:Int,val right:Int,val bottom:Int) {
    init { require(right>left && bottom>top) }
    val width get()=right-left
    val height get()=bottom-top
    fun contains(x:Double,y:Double)=x.isFinite() && y.isFinite() && x>=left && y>=top && x<right && y<bottom
    fun intersectsSegment(a:PixelPoint,b:PixelPoint):Boolean {
        if(!a.x.isFinite() || !a.y.isFinite() || !b.x.isFinite() || !b.y.isFinite())return false
        var low=0.0;var high=1.0
        fun clip(origin:Double,delta:Double,start:Double,end:Double):Boolean {
            if(delta==0.0)return origin in start..end
            val first=(start-origin)/delta;val last=(end-origin)/delta
            low=maxOf(low,minOf(first,last));high=minOf(high,maxOf(first,last))
            return low<=high
        }
        return clip(a.x,b.x-a.x,left.toDouble(),right.toDouble()) && clip(a.y,b.y-a.y,top.toDouble(),bottom.toDouble())
    }
    fun intersect(other:PixelRect):PixelRect? {
        val l=maxOf(left,other.left); val t=maxOf(top,other.top)
        val r=minOf(right,other.right); val b=minOf(bottom,other.bottom)
        return if(r>l && b>t) PixelRect(l,t,r,b) else null
    }
}
data class PixelPoint(val x:Double,val y:Double)
data class DisplayGeometry(val bounds:PixelRect,val gameWindow:PixelRect,val densityDpi:Int,val rotation:Int)

/** Full-display MediaProjection scales uniformly, then centers in its surface. This is
 * a projection transform, NOT a guessed game aspect ratio or a device profile. */
data class ProjectionTransform(val capture:PixelSize,val display:PixelRect) {
    val captureScale=min(capture.width.toDouble()/display.width,capture.height.toDouble()/display.height)
    val left=(capture.width-display.width*captureScale)/2.0
    val top=(capture.height-display.height*captureScale)/2.0
    fun toDisplay(x:Double,y:Double)=PixelPoint(display.left+(x-left)/captureScale,display.top+(y-top)/captureScale)
    fun toCapture(x:Double,y:Double)=PixelPoint(left+(x-display.left)*captureScale,top+(y-display.top)*captureScale)
    fun captureRect(rect:PixelRect):PixelRect? {
        val a=toCapture(rect.left.toDouble(),rect.top.toDouble())
        val b=toCapture(rect.right.toDouble(),rect.bottom.toDouble())
        // Never sample projection padding or pixels outside the app window.
        val l=ceil(a.x).toInt().coerceIn(0,capture.width); val t=ceil(a.y).toInt().coerceIn(0,capture.height)
        val r=floor(b.x).toInt().coerceIn(0,capture.width); val bottom=floor(b.y).toInt().coerceIn(0,capture.height)
        return if(r>l && bottom>t) PixelRect(l,t,r,bottom) else null
    }
}

/** Removes only proven near-black outer strips, not arbitrary dark gameplay cells.
 * Window/nav bounds come from Android; no ratios/resolutions/device names are stored. */
object VisibleGameArea {
    fun detect(frame:PixelFrame,window:PixelRect=PixelRect(0,0,frame.width,frame.height),occlusions:List<PixelRect> = emptyList()):PixelRect? {
        fun blackLine(horizontal:Boolean,at:Int,start:Int,end:Int):Boolean {
            val step=maxOf(1,(end-start)/180); var dark=0; var total=0
            for(n in start until end step step) {
                val x=if(horizontal)n else at;val y=if(horizontal)at else n
                if(occlusions.any { it.contains(x.toDouble(),y.toDouble()) })continue
                val p=if(horizontal) frame.rgbAt(n,at) else frame.rgbAt(at,n)
                total++; if(maxOf(p.red,p.green,p.blue)<=7) dark++
            }
            return total>=minOf(8,(end-start)/step) && total>0 && dark.toDouble()/total>=.995
        }
        var l=window.left; var t=window.top; var r=window.right; var b=window.bottom
        while(t<b && blackLine(true,t,l,r))t++
        while(b>t && blackLine(true,b-1,l,r))b--
        if(b<=t)return null
        while(l<r && blackLine(false,l,t,b))l++
        while(r>l && blackLine(false,r-1,t,b))r--
        if(r<=l)return null
        // Portrait games can occupy a small share of a landscape/tablet window. Area/aspect
        // cannot establish identity: the existing feature recognizers must confirm the UI.
        if(r-l<4 || b-t<4)return null
        return PixelRect(l,t,r,b)
    }
}

data class FrameGeometry(val capture:PixelSize,val display:DisplayGeometry,val gameInCapture:PixelRect) {
    init {
        require(gameInCapture.left>=0 && gameInCapture.top>=0 && gameInCapture.right<=capture.width && gameInCapture.bottom<=capture.height)
        require(display.gameWindow.intersect(display.bounds)==display.gameWindow)
    }
    val projection=ProjectionTransform(capture,display.bounds)
    val analysisSize=PixelSize(gameInCapture.width,gameInCapture.height)
    val scaleToDisplay get()=1.0/projection.captureScale
    val displayOrigin get()=projection.toDisplay(gameInCapture.left.toDouble(),gameInCapture.top.toDouble())
    fun map(x:Double,y:Double):PixelPoint? {
        if(!PixelRect(0,0,analysisSize.width,analysisSize.height).contains(x,y))return null
        val point=projection.toDisplay(gameInCapture.left+x,gameInCapture.top+y)
        return point.takeIf { display.bounds.contains(it.x,it.y) && display.gameWindow.contains(it.x,it.y) }
    }
}
data class GeometrySnapshot(val generation:Long,val geometry:FrameGeometry,val observedAt:Long)

/** A changed geometry revokes the old mapping immediately, before waiting for stability.
 * Every new session starts empty. Nothing is restored from preferences or persisted. */
class GeometryCalibration(private val stableFrames:Int=3,private val maxAgeMs:Long=2_000) {
    private var candidate:FrameGeometry?=null
    private var matches=0
    private var generation=0L
    private var confirmed:GeometrySnapshot?=null
    private var lastObservation:Long?=null
    @Synchronized fun reset() { generation++; candidate=null;matches=0;confirmed=null;lastObservation=null }
    @Synchronized fun observe(value:FrameGeometry?,now:Long):GeometrySnapshot? {
        if(value==null){reset();return null}
        if(lastObservation?.let {now-it !in 0..maxAgeMs}==true)reset()
        lastObservation=now
        if(candidate!=value){generation++;candidate=value;matches=1;confirmed=null}
        else matches=(matches+1).coerceAtMost(stableFrames)
        if(matches>=stableFrames)confirmed=GeometrySnapshot(generation,value,now)
        return confirmed
    }
    @Synchronized fun current(now:Long):GeometrySnapshot?=confirmed?.takeIf { now-it.observedAt in 0..maxAgeMs }
    @Synchronized fun valid(snapshot:GeometrySnapshot,now:Long)=current(now)?.generation==snapshot.generation
}

object FrameGeometryRegistry {
    val calibration=GeometryCalibration()
    private val caller=ThreadLocal<GeometrySnapshot?>()
    fun current(now:Long)=calibration.current(now)
    fun forCaller(now:Long)=caller.get() ?: current(now)
    fun <T> withSnapshot(snapshot:GeometrySnapshot,block:()->T):T {
        val previous=caller.get();caller.set(snapshot)
        try{return block()}finally{caller.set(previous)}
    }
    /** Capture the generation when scheduling, not when the delayed action executes. */
    fun bind(now:Long,clock:()->Long={android.os.SystemClock.elapsedRealtime()},action:()->Unit):()->Unit {
        val origin=forCaller(now)
        return { if(origin!=null && calibration.valid(origin,clock()))
            withSnapshot(origin,action) }
    }
}

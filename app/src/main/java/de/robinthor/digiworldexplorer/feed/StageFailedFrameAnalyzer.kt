package de.robinthor.digiworldexplorer.feed

import android.graphics.Color
import android.os.SystemClock
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.AnalysisImage as Image
import de.robinthor.digiworldexplorer.capture.FrameGeometryRegistry
import de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.vision.PixelFrame

/** Global foreground interruption, including user-started exclusive sessions. */
object StageFailedFrameAnalyzer {
    private val recovery=GrowthGuideRecovery()
    private var nextScanAt=0L
    private var nextNoTargetAt=0L
    @Volatile private var tapPending=false
    private var pendingSince=0L
    @Volatile private var epoch=0L
    @Volatile private var attemptToken=0L
    private val dismissalDispatch=ThreadLocal<Boolean>()
    val blocksOtherInput:Boolean get()=AutomationState.enabled && recovery.visible && dismissalDispatch.get()!=true
    fun analyze(image:Image,width:Int,height:Int):Boolean {
        if(!AutomationState.enabled){reset();return false}
        val now=SystemClock.elapsedRealtime()
        if(now<nextScanAt)return recovery.visible
        nextScanAt=now+500L
        val plane=image.planes.firstOrNull() ?: return recovery.visible
        val w=minOf(width,image.width);val h=minOf(height,image.height)
        if(w<=0 || h<=0 || plane.pixelStride<3 ||
            (h-1L)*plane.rowStride+(w-1L)*plane.pixelStride+2>=plane.buffer.limit())return recovery.visible
        val bytes=plane.buffer
        val frame=PixelFrame(w,h) {x,y->
            val i=y*plane.rowStride+x*plane.pixelStride
            Color.rgb(bytes.get(i).toInt() and 255,bytes.get(i+1).toInt() and 255,bytes.get(i+2).toInt() and 255)
        }
        val reading=GrowthGuideDetector.observe(frame)
        val wasVisible=recovery.visible
        recovery.observe(reading!=null,now)
        if(reading==null) {
            if(wasVisible){epoch++;tapPending=false;FeedFrameAnalyzer.allowImmediateScan();PersistentDiagnosticLog.record("GROWTH_GUIDE.GONE","dialog absent in fresh scan; previous input completion alone was not dismissal proof")}
            return false
        }
        AutoMoveController.pauseForPurchaseScreen()
        if(!wasVisible){PersistentDiagnosticLog.requestScreenshot("growth-guide-detected");PersistentDiagnosticLog.record("GROWTH_GUIDE.DETECTED","header=${reading.header} titleRed=${reading.titleRed} gray=${reading.headerGray} guideRed=${reading.guideRed}")}
        val service=DigiWorldAccessibilityService.instance ?: return true
        if(!wasVisible)service.clearCalibrationOverlay()
        service.updateStatusKeepingGrid(service.getString(R.string.overlay_stage_failed),true)
        if(tapPending && now-pendingSince>2_500L){attemptToken++;tapPending=false;PersistentDiagnosticLog.record("GROWTH_GUIDE.INPUT_TIMEOUT","no callback; delivery uncertain, attempt remains consumed")}
        if(!tapPending && recovery.mayTap(now) && now>=nextNoTargetAt) {
            val geometry=FrameGeometryRegistry.forCaller(now)
            val overlays=service.controlOverlayBounds()
            val target=reading.dismissTargets.firstOrNull {p->
                geometry?.geometry?.map(p.x,p.y)?.let {physical->overlays.none {it.contains(physical.x,physical.y)}}==true
            }
            if(target==null) {
                nextNoTargetAt=now+2_000
                service.updateStatusKeepingGrid(service.getString(R.string.overlay_growth_guide_blocked),true)
                PersistentDiagnosticLog.record("GROWTH_GUIDE.BLOCKED","no proven unobstructed dark backdrop header=${reading.header} candidates=${reading.dismissTargets} overlays=$overlays")
                return true
            }
            recovery.dispatched(now);tapPending=true;pendingSince=now
            val currentEpoch=epoch
            val currentToken=++attemptToken
            PersistentDiagnosticLog.record("GROWTH_GUIDE.DISMISS","attempt=${recovery.attempts} target=$target header=${reading.header} generation=${geometry?.generation}; awaiting fresh absence")
            dismissalDispatch.set(true)
            try {
                service.dispatchValidatedTap(target.x.toFloat(),target.y.toFloat()) {ok->
                    if(epoch==currentEpoch && attemptToken==currentToken){tapPending=false;if(!ok)recovery.rejected()}
                    PersistentDiagnosticLog.record("GROWTH_GUIDE.INPUT","completed=$ok; visual absence still required")
                }
            } finally {dismissalDispatch.remove()}
        } else if(recovery.attempts>=3) {
            service.updateStatusKeepingGrid(service.getString(R.string.overlay_growth_guide_blocked),true)
            PersistentDiagnosticLog.record("GROWTH_GUIDE.BLOCKED","three dismissal attempts; dialog still visible; no further blind taps")
        }
        return true
    }
    fun detect(width:Int,height:Int,pixel:(Int,Int)->Int):Boolean =
        width>0 && height>0 && GrowthGuideDetector.observe(PixelFrame(width,height,pixel))!=null
    fun reset(){epoch++;recovery.reset();nextScanAt=0L;nextNoTargetAt=0L;tapPending=false}
}

package de.robinthor.digiworldexplorer.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.Image
import de.robinthor.digiworldexplorer.detection.CellClassifier
import de.robinthor.digiworldexplorer.detection.CalibrationValidator
import de.robinthor.digiworldexplorer.detection.GridDetector
import de.robinthor.digiworldexplorer.detection.PreviewClassifier
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import java.io.File
import java.io.FileOutputStream

object CaptureFrameAnalyzer {
    /** Kandidatenschwelle. [GridDetector] prueft bereits Aequidistanz, Seitenverhaeltnis, Abdeckung
     *  und ein Kontrastverhaeltnis von >=5 zum Hintergrund. Die Konfidenz misst nur, wie klar die
     *  Linien sind - und das schwankt stark mit der Szenengrafik des Spiels (helle Szene ~0.84,
     *  dunkle Nachtszene ~0.74 bei identischer, korrekter Geometrie). Eine hohe Schwelle verwirft
     *  hier also korrekte Erkennungen. Die eigentliche Absicherung ist [STABLE_FRAMES]. */
    private const val CANDIDATE_MIN = .55
    /** So viele aufeinanderfolgende Analysen muessen dieselben Bounds liefern, bevor kalibriert wird. */
    private const val STABLE_FRAMES = 3
    /** Zulaessige Abweichung je Kante zwischen zwei Frames in Pixeln. */
    private const val BOUNDS_TOLERANCE = 4
    /** Mindestabstand zwischen zwei Diagnose-PNGs in Millisekunden. */
    private const val DIAGNOSTIC_INTERVAL = 60_000L
    @Volatile private var lastDiagnostic = 0L

    @Volatile private var calibrated: de.robinthor.digiworldexplorer.detection.GridDetection? = null
    @Volatile private var pending: de.robinthor.digiworldexplorer.detection.GridDetection? = null
    @Volatile private var pendingCount = 0
    /** Solange keine Kalibrierung steht, sucht [GridDetector] noch nach Kanten und darf das Overlay
     *  nicht im Bild haben. Danach sind die Bounds fixiert und das Overlay kann sichtbar bleiben. */
    val isCalibrated: Boolean get() = calibrated != null
    fun resetCalibration(){ calibrated=null; pending=null; pendingCount=0; de.robinthor.digiworldexplorer.strategy.AutoMoveController.reset(); de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService.instance?.clearCalibrationOverlay() }
    data class Result(val detected:Boolean,val confidence:Double,val output:String)

    private fun stable(candidate: de.robinthor.digiworldexplorer.detection.GridDetection): Boolean {
        val previous = pending
        val same = previous != null &&
            Math.abs(previous.bounds.left - candidate.bounds.left) <= BOUNDS_TOLERANCE &&
            Math.abs(previous.bounds.top - candidate.bounds.top) <= BOUNDS_TOLERANCE &&
            Math.abs(previous.bounds.right - candidate.bounds.right) <= BOUNDS_TOLERANCE &&
            Math.abs(previous.bounds.bottom - candidate.bounds.bottom) <= BOUNDS_TOLERANCE
        if (same) pendingCount++ else { pending = candidate; pendingCount = 1 }
        return pendingCount >= STABLE_FRAMES
    }

    fun analyze(context:Context,image:Image,width:Int,height:Int):Result?=runCatching{
        val plane=image.planes.first()
        val paddedWidth=plane.rowStride/plane.pixelStride
        val padded=Bitmap.createBitmap(paddedWidth,height,Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val frame=Bitmap.createBitmap(padded,0,0,width,height)
        if(frame!==padded)padded.recycle()

        val pixels=IntArray(width*height)
        frame.getPixels(pixels,0,width,0,0,width,height)
        var detection=calibrated ?: GridDetector.detect(width,height,pixels)?.takeIf { it.confidence>=CANDIDATE_MIN }
        var cells=detection?.let { CellClassifier.classify(width,height,pixels,it.bounds) }
        if(calibrated==null&&detection!=null){
            val geometryStable=stable(detection)
            if(cells!=null&&CalibrationValidator.plausible(cells)&&geometryStable){
                val locked=detection.copy(confidence=maxOf(detection.confidence,.90),reason="Raster ueber $STABLE_FRAMES Frames stabil")
                calibrated=locked
                detection=locked
                android.util.Log.i("DigiWorldCapture","kalibriert bounds=${locked.bounds} rohkonfidenz=${locked.confidence}")
            } else { detection=null;cells=null }
        }
        val preview=detection?.let { PreviewClassifier.classify(width,height,pixels,it.bounds) } ?: emptyMap()
        val directory=File(context.getExternalFilesDir(null) ?: context.filesDir,"diagnostics").apply{mkdirs()}
        var gameVisible=false
        if(detection!=null && cells!=null){
            val player=cells.maxByOrNull { it.value.player }
            val playerCell=player?.key
            val items=cells.filter { (cell,score) -> cell!=playerCell && score.item>.06 }.keys.sortedWith(compareBy({it.row},{it.col}))
            val obstacles=cells.filter { (cell,score) -> cell!=playerCell && score.obstacle() }.keys.sortedWith(compareBy({it.row},{it.col}))
            val detectedDashButton=de.robinthor.digiworldexplorer.detection.DashButtonLocator.locate(width,height,pixels,detection.bounds)
            val dashButton=detectedDashButton ?: if(de.robinthor.digiworldexplorer.strategy.AutomationState.dwsNavigationSettings.dashSpamUntilZero)
                de.robinthor.digiworldexplorer.detection.DashButtonLocator.relativeFallback(width,height,detection.bounds)
            else null
            val hud=de.robinthor.digiworldexplorer.detection.HudCounterReader.read(width,height,pixels,detection.bounds)
            // Aufgezeichnet sind bisher nur die Ziffern 1 und 2. Unbekannte Formen werden hier
            // ausgegeben, damit die fehlenden Vorlagen aus echten Spielstaenden ergaenzt werden koennen.
            if(hud.unknown.isNotEmpty())android.util.Log.i("DigiWorldHud","unbekannte Ziffer "+hud.unknown)
            val summary="confidence="+detection.confidence+"\nplayer="+player?.key+" score="+player?.value?.player+"\nitems="+items+"\nobstacles="+obstacles+"\npreview="+preview+"\ndash="+dashButton+"\nkrallen="+hud.claws+" dashvorrat="+hud.dash+"\n"
            if(android.os.SystemClock.elapsedRealtime()-lastDiagnostic>=DIAGNOSTIC_INTERVAL){
                File(directory,"latest_detection.txt").writeText(summary)
                android.util.Log.i("DigiWorldDetection",summary.replace("\n","; "))
            }
            gameVisible=CalibrationValidator.plausible(cells)
            if(gameVisible) AutoMoveController.onAnalysis(detection.confidence,detection.bounds,cells,preview,dashButton,hud)
            else AutoMoveController.onAnalysis(0.0,detection.bounds,emptyMap(),emptyMap(),null,hud)
        }
        // Der PNG-Export kostet bei 1080x2400 mehr als die gesamte Analyse. Nach der Kalibrierung
        // laeuft diese mit rund 3 Hz, deshalb wird der Diagnoseframe nur noch gedrosselt geschrieben.
        val output=File(directory,"dynamic_grid.png")
        val now=android.os.SystemClock.elapsedRealtime()
        if(now-lastDiagnostic>=DIAGNOSTIC_INTERVAL){
            lastDiagnostic=now
            val diagnostic=frame.copy(Bitmap.Config.ARGB_8888,true)
            val canvas=Canvas(diagnostic)
            val paint=Paint().apply{color=Color.GREEN;style=Paint.Style.STROKE;strokeWidth=(width/180f).coerceAtLeast(3f)}
            detection?.bounds?.let{b->
                canvas.drawRect(b.left.toFloat(),b.top.toFloat(),b.right.toFloat(),b.bottom.toFloat(),paint)
                for(i in 1..4){
                    val x=b.left+(b.right-b.left)*i/5f
                    val y=b.top+(b.bottom-b.top)*i/5f
                    canvas.drawLine(x,b.top.toFloat(),x,b.bottom.toFloat(),paint)
                    canvas.drawLine(b.left.toFloat(),y,b.right.toFloat(),y,paint)
                }
            }
            FileOutputStream(output).use{diagnostic.compress(Bitmap.CompressFormat.PNG,90,it)}
            diagnostic.recycle()
        }
        frame.recycle()
        Result(gameVisible,detection?.confidence?:0.0,output.absolutePath)
    }.onFailure{ android.util.Log.e("DigiWorldCapture","analyze failed",it) }.getOrNull()
}

package de.robinthor.digiworldexplorer.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.Image
import de.robinthor.digiworldexplorer.detection.CellClassifier
import de.robinthor.digiworldexplorer.detection.GridDetector
import java.io.File
import java.io.FileOutputStream

object CaptureFrameAnalyzer {
    data class Result(val detected:Boolean,val confidence:Double,val output:String)

    fun analyze(context:Context,image:Image,width:Int,height:Int):Result?=runCatching{
        val plane=image.planes.first()
        val paddedWidth=plane.rowStride/plane.pixelStride
        val padded=Bitmap.createBitmap(paddedWidth,height,Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val frame=Bitmap.createBitmap(padded,0,0,width,height)
        if(frame!==padded)padded.recycle()

        val pixels=IntArray(width*height)
        frame.getPixels(pixels,0,width,0,0,width,height)
        val detection=GridDetector.detect(width,height,pixels)
        val cells=detection?.let { CellClassifier.classify(width,height,pixels,it.bounds) }
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
        val directory=File(context.getExternalFilesDir(null) ?: context.filesDir,"diagnostics").apply{mkdirs()}
        if(detection!=null && cells!=null){
            val player=cells.maxByOrNull { it.value.player }
            val playerCell=player?.key
            val items=cells.filter { (cell,score) -> cell!=playerCell && score.item>.06 }.keys.sortedWith(compareBy({it.row},{it.col}))
            val obstacles=cells.filter { (cell,score) -> cell!=playerCell && score.obstacle() }.keys.sortedWith(compareBy({it.row},{it.col}))
            val summary="confidence="+detection.confidence+"\nplayer="+player?.key+" score="+player?.value?.player+"\nitems="+items+"\nobstacles="+obstacles+"\n"
            File(directory,"latest_detection.txt").writeText(summary)
            android.util.Log.i("DigiWorldDetection",summary.replace("\n","; "))
        }
        val output=File(directory,"dynamic_grid.png")
        FileOutputStream(output).use{diagnostic.compress(Bitmap.CompressFormat.PNG,100,it)}
        diagnostic.recycle();frame.recycle()
        Result(detection!=null,detection?.confidence?:0.0,output.absolutePath)
    }.getOrNull()
}

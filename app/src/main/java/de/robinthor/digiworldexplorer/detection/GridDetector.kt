package de.robinthor.digiworldexplorer.detection

import kotlin.math.abs
import kotlin.math.roundToInt

data class GridBounds(val left:Int,val top:Int,val right:Int,val bottom:Int)
data class GridDetection(val confidence:Double,val bounds:GridBounds,val reason:String)

object GridDetector {
    fun detect(width:Int,height:Int,argb:IntArray):GridDetection? {
        require(width>0&&height>0&&argb.size==width*height)
        val xScore=DoubleArray(width-1)
        val yScore=DoubleArray(height-1)
        val y0=(height*.30).toInt();val y1=(height*.70).toInt()
        for(y in y0 until y1) for(x in 0 until width-1)
            xScore[x]+=abs(gray(argb[y*width+x])-gray(argb[y*width+x+1])).toDouble()
        val xRows=(y1-y0).coerceAtLeast(1);for(i in xScore.indices)xScore[i]/=xRows
        val x0=(width*.10).toInt();val x1=(width*.90).toInt()
        for(y in 0 until height-1) for(x in x0 until x1)
            yScore[y]+=abs(gray(argb[y*width+x])-gray(argb[(y+1)*width+x])).toDouble()
        val yCols=(x1-x0).coerceAtLeast(1);for(i in yScore.indices)yScore[i]/=yCols
        // Das Spielfeld kann den Bildschirm nahezu randlos ausfuellen (auf 20:9 beginnt es bei ~2,5% der Breite).
        // Die Startsuche darf deshalb nicht bei 5% der Breite beginnen, sonst wird das linke Raster nie gefunden.
        val xb=bestSix(xScore,0 until (width*.25).toInt(),(width*.10).toInt() until (width*.22).toInt())?:return null
        val yb=bestSix(yScore,(height*.14).toInt() until (height*.42).toInt(),(height*.045).toInt() until (height*.10).toInt())?:return null
        if(xb.values.sorted()[1]<10.0||yb.values.sorted()[1]<10.0)return null
        // Absolute Gradientenwerte haengen stark von Kunststil und Renderskalierung ab
        // (Spielfeld-Trennlinien liegen real bei ~20, synthetische Testbilder bei >200).
        // Aussagekraeftig ist deshalb das Verhaeltnis der Rasterkanten zum Hintergrundgradienten.
        val xRatio=xb.values.sorted()[1]/xScore.average().coerceAtLeast(.01)
        val yRatio=yb.values.sorted()[1]/yScore.average().coerceAtLeast(.01)
        if(xRatio<5.0||yRatio<5.0) {
            // Current game UI and some scaled phone boards keep clear columns but obscure rows.
            // This is shared UI observation; the V4/V5 movement rules stay separate.
            return bottomAnchoredPhoneGrid(width,height,xb,xRatio,yScore,argb)
        }
        val xf=fit(xb.positions);val yf=fit(yb.positions)
        val b=GridBounds(xf.second.roundToInt(),yf.second.roundToInt(),(xf.second+5*xf.first).roundToInt(),(yf.second+5*yf.first).roundToInt())
        val bw=b.right-b.left;val bh=b.bottom-b.top
        val aspect=bw/bh.coerceAtLeast(1).toDouble();val coverage=bw*bh/(width*height).toDouble()
        if(aspect !in .85..1.55||coverage !in .20..0.45)return null
        // The reward/progress strip supplies a tempting sixth edge, shifting the board
        // down one row. Reject that candidate, then keep searching actual board bottoms.
        if(hasTextFooter(width,height,argb,b)) return bottomAnchoredPhoneGrid(width,height,xb,xRatio,yScore,argb)
        val anchored=bottomAnchoredPhoneGrid(width,height,xb,xRatio,yScore,argb)
        if(anchored!=null && b.top-anchored.bounds.top>height*.035 &&
            abs(b.bottom-anchored.bounds.bottom)<height*.06) return anchored
        return GridDetection((.70+.03*(minOf(xRatio,yRatio)-4.0)).coerceIn(.0,.98),b,"six equidistant grid edges")
    }
    private fun bottomAnchoredPhoneGrid(width:Int,height:Int,xb:Six,xRatio:Double,yScore:DoubleArray,argb:IntArray):GridDetection? {
        if(xRatio<3.0||xb.values.sorted()[1]<12.0)return null
        originalPhoneAnchor(width,height,xb,yScore)?.takeUnless { hasTextFooter(width,height,argb,it.bounds) }?.let{return it}
        val xFit=fit(xb.positions)
        val cellWidth=xFit.first
        if(cellWidth<=0.0)return null
        val bottomRange=(height*.60).toInt()..(height*.715).toInt().coerceAtMost(yScore.lastIndex)
        if(bottomRange.isEmpty())return null
        val background=yScore.average().coerceAtLeast(.01)
        val bottomCandidates=mutableListOf<Int>()
        for(at in bottomRange.sortedByDescending { yScore[it] }){
            if(yScore[at]<maxOf(18.0,background*6.0))break
            if(bottomCandidates.none{abs(it-at)<7})bottomCandidates+=at
            if(bottomCandidates.size>=16)break
        }
        fun nearbyScore(at:Int):Double = ((at-3).coerceAtLeast(0)..(at+3).coerceAtMost(yScore.lastIndex))
            .maxOf { yScore[it] }
        for(bottom in bottomCandidates){
            val top=listOf(.82,.92).mapNotNull { heightFactor ->
                val estimatedCellHeight=cellWidth*heightFactor
                val estimatedTop=(bottom-5*estimatedCellHeight).roundToInt()
                val tolerance=(estimatedCellHeight*.12).roundToInt().coerceAtLeast(4)
                val range=(estimatedTop-tolerance).coerceAtLeast(0)..
                    (estimatedTop+tolerance).coerceAtMost(yScore.lastIndex)
                range.maxByOrNull { yScore[it] }
            }.filter { candidate ->
                val step=(bottom-candidate)/5.0
                candidate in (height*.20).toInt()..(height*.40).toInt() &&
                    nearbyScore(candidate)>=background*2.8 &&
                    (1..4).count { row -> nearbyScore((candidate+row*step).roundToInt())>=background*2.5 }>=2
            }.maxByOrNull { yScore[it] } ?: continue
            val bounds=GridBounds(xFit.second.roundToInt(),top,
                (xFit.second+5*cellWidth).roundToInt(),bottom)
            val bw=bounds.right-bounds.left;val bh=bounds.bottom-bounds.top
            val aspect=bw/bh.coerceAtLeast(1).toDouble();val coverage=bw*bh/(width*height).toDouble()
            if(hasTextFooter(width,height,argb,bounds)) continue
            if(bounds.left>=0&&bounds.right<=width&&aspect in .85..1.55&&coverage in .20.. .45)
                return GridDetection(.62,bounds,"bottom-anchored phone grid")
        }
        return null
    }
    private fun hasTextFooter(width:Int,height:Int,argb:IntArray,bounds:GridBounds):Boolean =
        CellClassifier.classify(width,height,argb,bounds,allSprites=false,legacyV4Core=true)
            .count { (cell,score) -> cell.row==4 && score.text>.08 } >= 3
    /** Keep the pre-update phone fit before trying the lower, more compact 1.5 UI. */
    private fun originalPhoneAnchor(width:Int,height:Int,xb:Six,yScore:DoubleArray):GridDetection?{
        val xFit=fit(xb.positions);val cellWidth=xFit.first
        if(cellWidth<=0.0)return null
        val range=(height*.67).toInt()..(height*.715).toInt().coerceAtMost(yScore.lastIndex)
        if(range.isEmpty())return null
        val bottom=range.maxByOrNull{yScore[it]}?:return null
        val background=yScore.average().coerceAtLeast(.01)
        if(yScore[bottom]<maxOf(18.0,background*6.0))return null
        val estimatedCellHeight=cellWidth*.92
        val estimatedTop=(bottom-5*estimatedCellHeight).roundToInt()
        val tolerance=(estimatedCellHeight*.12).roundToInt().coerceAtLeast(4)
        val topRange=(estimatedTop-tolerance).coerceAtLeast(0)..
            (estimatedTop+tolerance).coerceAtMost(yScore.lastIndex)
        val top=topRange.maxByOrNull{yScore[it]}?:return null
        if(top !in (height*.20).toInt()..(height*.40).toInt())return null
        fun nearbyScore(at:Int):Double=((at-3).coerceAtLeast(0)..(at+3).coerceAtMost(yScore.lastIndex)).maxOf{yScore[it]}
        if(nearbyScore(top)<background*2.8||nearbyScore((bottom-(bottom-top)/5.0).roundToInt())<background*3.0)return null
        val bounds=GridBounds(xFit.second.roundToInt(),top,(xFit.second+5*cellWidth).roundToInt(),bottom)
        val bw=bounds.right-bounds.left;val bh=bounds.bottom-bounds.top
        val aspect=bw/bh.coerceAtLeast(1).toDouble();val coverage=bw*bh/(width*height).toDouble()
        if(bounds.left<0||bounds.right>width||aspect !in .85..1.55||coverage !in .20.. .45)return null
        return GridDetection(.62,bounds,"bottom-anchored phone grid")
    }
    private data class Six(val quality:Double,val positions:IntArray,val values:DoubleArray)
    private fun bestSix(score:DoubleArray,starts:IntRange,steps:IntRange):Six? {
        var best:Six?=null
        for(step in steps)for(start in starts){
            if(start+5*step>=score.size)continue
            val pos=IntArray(6);val values=DoubleArray(6)
            for(i in 0..5){val expected=start+i*step;val lo=(expected-2).coerceAtLeast(0);val hi=(expected+2).coerceAtMost(score.lastIndex);var at=lo
                for(p in lo..hi)if(score[p]>score[at])at=p
                pos[i]=at;values[i]=score[at]}
            val quality=values.sorted()[1]+values.average()*.10
            if(best==null||quality>best.quality)best=Six(quality,pos,values)
        }
        return best
    }
    private fun fit(p:IntArray):Pair<Double,Double>{
        val meanX=2.5;val meanY=p.average();var num=0.0;var den=0.0
        for(i in 0..5){num+=(i-meanX)*(p[i]-meanY);den+=(i-meanX)*(i-meanX)}
        val slope=num/den;return slope to (meanY-slope*meanX)
    }
    private fun gray(pixel:Int):Int=(((pixel shr 16)and 255)+((pixel shr 8)and 255)+(pixel and 255))/3
}

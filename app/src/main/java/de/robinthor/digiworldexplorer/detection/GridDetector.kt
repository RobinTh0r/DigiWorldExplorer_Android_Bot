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
        val xb=bestSix(xScore,(width*.05).toInt() until (width*.25).toInt(),(width*.10).toInt() until (width*.22).toInt())?:return null
        val yb=bestSix(yScore,(height*.20).toInt() until (height*.39).toInt(),(height*.045).toInt() until (height*.10).toInt())?:return null
        if(xb.values.sorted()[1]<15.0||yb.values.sorted()[1]<15.0)return null
        val xf=fit(xb.positions);val yf=fit(yb.positions)
        val b=GridBounds(xf.second.roundToInt(),yf.second.roundToInt(),(xf.second+5*xf.first).roundToInt(),(yf.second+5*yf.first).roundToInt())
        val bw=b.right-b.left;val bh=b.bottom-b.top
        val aspect=bw/bh.coerceAtLeast(1).toDouble();val coverage=bw*bh/(width*height).toDouble()
        if(aspect !in .85..1.55||coverage !in .20..0.45)return null
        return GridDetection((.70+.005*minOf(xb.quality,yb.quality)).coerceAtMost(.98),b,"six equidistant grid edges")
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

package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.capture.PixelPoint
import de.robinthor.digiworldexplorer.capture.PixelRect
import de.robinthor.digiworldexplorer.vision.PixelFrame
import kotlin.math.abs

data class GrowthGuideReading(val header:PixelRect,val dismissTargets:List<PixelPoint>,
    val titleRed:Double,val headerGray:Double,val guideRed:Double)

/** This specific guide, not an arbitrary red modal; measured outside-header targets. */
object GrowthGuideDetector {
    fun observe(frame:PixelFrame):GrowthGuideReading? {
        val v=frame.observedViewport
        val step=maxOf(2,v.width/240)
        fun gray(p:Int):Boolean {
            val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255
            return r in 90..180 && abs(r-g)<28 && abs(g-b)<28
        }
        fun ratio(x0:Double,y0:Double,x1:Double,y1:Double,match:(Int)->Boolean):Double {
            var hits=0;var total=0
            for(y in v.top+(v.height*y0).toInt() until v.top+(v.height*y1).toInt() step step)
                for(x in v.left+(v.width*x0).toInt() until v.left+(v.width*x1).toInt() step step) {
                    total++;if(match(frame.argbAt(x,y)))hits++
                }
            return hits.toDouble()/total.coerceAtLeast(1)
        }
        val titleRed=ratio(.08,.11,.92,.23) {p->val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255;r>120 && r>g*1.30 && r>b*1.15}
        if(titleRed<.08)return null
        val headerGray=ratio(.08,.21,.92,.30,::gray)
        if(headerGray<.28)return null
        val guideRed=ratio(.10,.28,.90,.82) {p->val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255;r>105 && r>g*1.25 && r>b*1.10}
        if(guideRed<.02)return null
        data class Row(val y:Int,val left:Int,val right:Int)
        val rows=mutableListOf<Row>()
        val fromX=v.left+(v.width*.05).toInt();val toX=v.left+(v.width*.95).toInt()
        for(y in v.top+(v.height*.20).toInt() until v.top+(v.height*.40).toInt() step step) {
            val xs=(fromX until toX step step).filter {gray(frame.argbAt(it,y))}
            if(xs.size*step>v.width*.55 && xs.last()-xs.first()>v.width*.60)
                rows+=Row(y,xs.first(),xs.last()+step)
        }
        val bands=mutableListOf<MutableList<Row>>()
        for(row in rows) {
            if(bands.lastOrNull()?.last()?.y!=row.y-step)bands.add(mutableListOf<Row>())
            bands.last().add(row)
        }
        val band=bands.filter { (it.last().y-it.first().y+step).toDouble()/v.height in .025.. .11 }
            .maxByOrNull {it.size} ?: return null
        val header=PixelRect(band.minOf {it.left},band.first().y,
            minOf(v.left+v.width,band.maxOf {it.right}),minOf(v.top+v.height,band.last().y+step))
        val y=(header.top+header.bottom)/2.0+v.height*.20
        val targets=listOf(PixelPoint((header.right+v.left+v.width)/2.0,y),PixelPoint((v.left+header.left)/2.0,y))
            .filter {point->
                val margin=maxOf(2,step*2)
                val outside=point.x<header.left-margin || point.x>header.right+margin
                outside && point.y<v.top+v.height && (-1..1).all {dy->(-1..1).all {dx->
                    val p=frame.rgbAt((point.x+dx*margin).toInt(),(point.y+dy*margin).toInt())
                    maxOf(p.red,p.green,p.blue)<85
                }}
            }
        return GrowthGuideReading(header,targets,titleRed,headerGray,guideRed)
    }
}

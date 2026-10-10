package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*

/** Closed cyan speech frame containing a pale panel and icon; never a projectile core. */
object BondBubbleDetector {
    fun detect(frame: PixelFrame): NormalizedPoint? {
        val viewport = GameViewport.detect(frame)
        val w = 360; val h = 640
        val cyan = BooleanArray(w*h)
        // Partner position and phone aspect ratio move the bubble farther horizontally than the
        // original single reference frame. Keep the vertical Home-stage band but cover the full
        // central partner area.
        for (y in 166..320) for (x in 108..260) {
            val p = frame.rgbAt(viewport.left+x*viewport.width/w, viewport.top+y*viewport.height/h)
            val hsv = p.hsv()
            cyan[y*w+x] = hsv.hue in 84..106 && hsv.saturation >= 80 && hsv.value >= 145
        }
        fun cyanAt(x:Int,y:Int)=x in 0 until w && y in 0 until h && cyan[y*w+x]
        // Anti-aliasing splits the thin outline into arcs. Pair bounded arcs to propose
        // complete frames; dilation would also attach unrelated battle streaks nearby.
        val arcs=ColorComponents.find(cyan,w,h).filter { it.pixels>=8 && it.width<=38 && it.height<=28 }
        val boxes=buildList {
            addAll(arcs)
            for(i in arcs.indices)for(j in i+1 until arcs.size) {
                val a=arcs[i];val b=arcs[j]
                add(ColorComponent(minOf(a.left,b.left),minOf(a.top,b.top),maxOf(a.right,b.right),maxOf(a.bottom,b.bottom),a.pixels+b.pixels))
            }
        }.distinct()
        val panels = boxes.filter {
            it.width in 12..38 && it.height in 10..28 && it.pixels >= 18
        }.filter { box ->
            val aspect=box.width.toDouble()*viewport.width*h/(box.height.toDouble()*viewport.height*w)
            if(aspect !in .70..1.80)return@filter false
            fun sideCoverage(horizontal:Boolean,at:Int,start:Int,length:Int):Double {
                val samples=(start+(length*.2).toInt())..(start+(length*.8).toInt())
                return samples.count { n -> (-2..2).any { d ->
                    if(horizontal)cyanAt(n,at+d) else cyanAt(at+d,n)
                } }.toDouble()/samples.count()
            }
            val sides=listOf(sideCoverage(true,box.top,box.left,box.width),sideCoverage(true,box.bottom,box.left,box.width),
                    sideCoverage(false,box.left,box.top,box.height),sideCoverage(false,box.right,box.top,box.height))
            if(sides.min()<.30 || sides.average()<.60)return@filter false
            var pale=0;var ink=0;var count=0
            for(y in box.top+2 until box.bottom-1)for(x in box.left+2 until box.right-1) {
                val hsv=frame.rgbAt(viewport.left+x*viewport.width/w,viewport.top+y*viewport.height/h).hsv()
                count++
                if(hsv.saturation<=60 && hsv.value>=185)pale++
                if(hsv.value<125 || (hsv.saturation>95 && hsv.hue !in 80..115))ink++
            }
            count>0 && pale.toDouble()/count>=.20 && ink.toDouble()/count>=.08
        }
        val panel = panels.maxByOrNull { it.width*it.height } ?: return null
        return NormalizedPoint((panel.left+panel.width/2.0)/w, (panel.top+panel.height/2.0)/h)
            .takeIf { it.y in .35.. .48 }
    }

    /** Tap the verified bubble itself. Tapping the figure underneath opens its Partner popup on
     * physical phones once the bubble disappears between scheduled taps. */
    fun tapTarget(bubble: NormalizedPoint): NormalizedPoint? {
        return bubble.takeIf { it.x in .30.. .72 && it.y in .25.. .52 }
    }
}

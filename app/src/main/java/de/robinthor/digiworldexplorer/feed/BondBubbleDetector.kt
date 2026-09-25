package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*

/** White speech panel plus cyan outline; battle damage text alone cannot authorize feeding. */
object BondBubbleDetector {
    fun detect(frame: PixelFrame): NormalizedPoint? {
        val viewport = GameViewport.fit(frame.width, frame.height)
        val w = 360; val h = 640
        val white = BooleanArray(w*h)
        val cyan = BooleanArray(w*h)
        for (y in 175..300) for (x in 150..225) {
            val p = frame.rgbAt(viewport.left+x*viewport.width/w, viewport.top+y*viewport.height/h)
            white[y*w+x] = minOf(p.red,p.green,p.blue) > 200 && maxOf(p.red,p.green,p.blue)-minOf(p.red,p.green,p.blue)<40
            cyan[y*w+x] = p.red < 100 && p.green > 170 && p.blue > 170
        }
        val panels = ColorComponents.find(white,w,h).filter {
            it.width in 12..32 && it.height in 10..25 && it.pixels >= 70
        }.filter { box ->
            var border = 0
            for (y in box.top-3..box.bottom+3) for (x in box.left-3..box.right+3)
                if (x in 0 until w && y in 0 until h && cyan[y*w+x]) border++
            border >= 15
        }
        val panel = panels.singleOrNull() ?: return null
        return NormalizedPoint((panel.left+panel.width/2.0)/w, (panel.top+panel.height/2.0)/h)
    }
}

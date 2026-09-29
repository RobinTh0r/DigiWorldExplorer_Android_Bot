package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*

/** White speech panel plus cyan outline; battle damage text alone cannot authorize feeding. */
object BondBubbleDetector {
    fun detect(frame: PixelFrame): NormalizedPoint? {
        val viewport = GameViewport.fit(frame.width, frame.height)
        val w = 360; val h = 640
        val white = BooleanArray(w*h)
        val cyan = BooleanArray(w*h)
        // Partner position and phone aspect ratio move the bubble farther horizontally than the
        // original single reference frame. Keep the vertical Home-stage band but cover the full
        // central partner area.
        for (y in 166..320) for (x in 130..252) {
            val p = frame.rgbAt(viewport.left+x*viewport.width/w, viewport.top+y*viewport.height/h)
            val hsv = p.hsv()
            white[y*w+x] = hsv.saturation <= 55 && hsv.value >= 190
            cyan[y*w+x] = hsv.hue in 84..106 && hsv.saturation >= 80 && hsv.value >= 145
        }
        val panels = ColorComponents.find(white,w,h).filter {
            it.width in 12..32 && it.height in 10..25 && it.pixels >= 70
        }.filter { box ->
            var border = 0
            for (y in box.top-3..box.bottom+3) for (x in box.left-3..box.right+3)
                if (x in 0 until w && y in 0 until h && cyan[y*w+x]) border++
            border >= 15
        }
        val panel = panels.maxByOrNull { it.pixels } ?: return null
        return NormalizedPoint((panel.left+panel.width/2.0)/w, (panel.top+panel.height/2.0)/h)
    }

    /** The collectible belongs to the figure below-left of its floating bubble. */
    fun tapTarget(bubble: NormalizedPoint): NormalizedPoint? {
        val target = NormalizedPoint(bubble.x - .071, bubble.y + .076)
        return target.takeIf { it.x in .30.. .60 && it.y in .32.. .54 }
    }
}

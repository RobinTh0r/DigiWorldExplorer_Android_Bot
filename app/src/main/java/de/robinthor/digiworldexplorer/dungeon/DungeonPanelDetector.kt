package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.*

data class DungeonPanel(val kind: String, val target: NormalizedPoint, val remaining: Int? = null)

/** Reads the foreground action and its counter, independently of the list behind the modal. */
object DungeonPanelDetector {
    fun detect(frame: PixelFrame, key: DungeonKey, v: GameViewport = GameViewport.fit(frame.width, frame.height)): DungeonPanel? {
        fun color(x: Double, y: Double, purple: Boolean = false): Double = frame.ratioInViewportPatch(v, NormalizedPoint(x,y), .018,.010) {
            val h = it.hsv()
            h.value >= 150 && h.saturation >= 100 && if (purple) h.hue in 120..155 else h.hue in 90..115
        }
        val titleY = when(key) { DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .196; DungeonKey.METAL_SEA -> .186; else -> .234 }
        val title = frame.ratioInViewportPatch(v,NormalizedPoint(.5,titleY),.22,.018) {
            val hsv=it.hsv(); hsv.hue in 90..115 && hsv.saturation >= 90 && hsv.value >= 100
        }
        if(title < .35) return null
        if (key == DungeonKey.NETWORK_DEFENSE) {
            if (color(.45,.59) > .45) return DungeonPanel("network_confirm", NormalizedPoint(.50,.59))
            if (color(.61,.59) > .45 && color(.34,.59, true) > .45)
                return DungeonPanel("network_leave", NormalizedPoint(.64,.59))
        }
        val y = when(key) { DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .790; DungeonKey.METAL_SEA -> .746; else -> .700 }
        val ticketY = when(key) { DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .145; DungeonKey.METAL_SEA -> .690; else -> .644 }
        val ticketX = if (key == DungeonKey.NETWORK_DEFENSE || key == DungeonKey.DAILY) .802 else .480
        val tickets = number(frame, v, ticketX, ticketX + .078, ticketY)
        if (key == DungeonKey.DAILY && color(.41,y,true) > .45)
            return DungeonPanel("destroy", NormalizedPoint(.5,y), tickets)
        if (color(.41,y,true) > .45 && color(.58,y,true) > .45)
            return DungeonPanel("ad", NormalizedPoint(.5,y), number(frame,v,.514,.582,y))
        if (key == DungeonKey.NETWORK_DEFENSE) {
            if (color(.59,.570) > .45)
                return DungeonPanel("network_challenge", NormalizedPoint(.65,.57), tickets)
            if (color(.41,.79) > .45) return DungeonPanel("network_matching", NormalizedPoint(.5,.79), tickets)
        } else if (color(.59,y) > .45) return DungeonPanel("challenge", NormalizedPoint(.66,y), tickets)
        return null
    }

    internal fun number(frame: PixelFrame, v: GameViewport, left: Double, right: Double, centerY: Double): Int? {
        val x0 = v.left + (v.width * left).toInt()
        val y0 = v.top + (v.height * (centerY-.013)).toInt()
        val w = (v.width * (right-left)).toInt().coerceAtLeast(1)
        val h = (v.height * .026).toInt().coerceAtLeast(1)
        val mask = BooleanArray(w*h) { i ->
            val c = frame.rgbAt(x0+i%w,y0+i/w)
            minOf(c.red,c.green,c.blue) >= 170 && maxOf(c.red,c.green,c.blue)-minOf(c.red,c.green,c.blue) < 65
        }
        val glyph = ColorComponents.find(mask,w,h).filter { it.height >= h*.35 && it.pixels >= 6 }
            .minByOrNull { it.left } ?: return null
        // Counters here are 0/1/2. Preserve only availability; spent attempts are
        // counted by the executor, never inferred from an approximate glyph label.
        val gw = glyph.width + 2
        val gh = glyph.height + 2
        val background = BooleanArray(gw*gh) { true }
        for(y in 0 until glyph.height) for(x in 0 until glyph.width)
            background[(y+1)*gw+x+1] = !mask[(glyph.top+y)*w+glyph.left+x]
        val hole = ColorComponents.find(background,gw,gh).any {
            it.left > 0 && it.top > 0 && it.right < gw-1 && it.bottom < gh-1 && it.pixels >= 2
        }
        return if (hole) 0 else 1
    }
}

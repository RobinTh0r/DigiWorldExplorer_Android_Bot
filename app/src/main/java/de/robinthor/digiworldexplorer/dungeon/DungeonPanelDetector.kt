package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.*

data class DungeonPanel(val kind: String, val target: NormalizedPoint, val remaining: Int? = null)

/** Reads the foreground action and its counter, independently of the list behind the modal. */
object DungeonPanelDetector {
    fun detect(frame: PixelFrame, key: DungeonKey, v: GameViewport = GameViewport.fit(frame.width, frame.height)): DungeonPanel? {
        fun color(x: Double, y: Double, purple: Boolean = false, rx: Double = .018, ry: Double = .010): Double = frame.ratioInViewportPatch(v, NormalizedPoint(x,y), rx,ry) {
            val h = it.hsv()
            h.value >= 150 && h.saturation >= 100 && if (purple) h.hue in 120..155 else h.hue in 90..115
        }
        val titleY = if (v.usesTallPhoneLayout) when(key) {
            DungeonKey.APOCALYMON_WALL -> .230
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .230
            DungeonKey.METAL_SEA -> .218
            else -> .270
        } else when(key) {
            DungeonKey.APOCALYMON_WALL -> .208
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .196
            DungeonKey.METAL_SEA -> .186
            else -> .234
        }
        val title = frame.ratioInViewportPatch(v,NormalizedPoint(.5,titleY),.22,.018) {
            val hsv=it.hsv(); hsv.hue in 90..115 && hsv.saturation >= 90 && hsv.value >= 100
        }
        if(title < .35) return null
        if (key == DungeonKey.NETWORK_DEFENSE) {
            fun cyanButton(rgb: Rgb): Boolean = rgb.blue > 120 && rgb.green > 75 &&
                rgb.blue > rgb.red * 1.15 && rgb.green > rgb.red * .90
            fun purpleButton(rgb: Rgb): Boolean { val h=rgb.hsv(); return h.hue in 120..170 && h.saturation>=90 && h.value>=120 }
            val area = NormalizedRect(.12,.30,.88,.86)
            val cyanButtons = ColorRegionLocator.find(frame,v,area,predicate=::cyanButton)
                .filter { it.width in .05..0.45 && it.height in .010..0.11 }
            val purpleButtons = ColorRegionLocator.find(frame,v,area,predicate=::purpleButton)
                .filter { it.width in .05..0.45 && it.height in .010..0.11 }
            fun pairedPurple(button: NormalizedRect) = purpleButtons.any {
                it.center.x < button.center.x && kotlin.math.abs(it.center.y-button.center.y) < .045
            }
            val dimmed = frame.ratioInViewportPatch(v,NormalizedPoint(.75,.14),.20,.04) {
                it.hsv().value < 100
            } > .88
            if (dimmed) {
                // The leave question has two side-by-side actions on the same row.  Do
                // not infer it from an absolute row: the row moves with dialog height.
                val leave = cyanButtons.filter { it.center.x > .50 && it.center.y in .48..0.70 && pairedPurple(it) }
                    .maxByOrNull { it.width*it.height }
                if (leave != null) return DungeonPanel("network_leave", leave.center)
                val confirm = cyanButtons.filter { it.center.x in .38..0.62 && it.center.y in .48..0.70 && !pairedPurple(it) }
                    .maxByOrNull { it.width*it.height }
                if (confirm != null) return DungeonPanel("network_confirm", confirm.center)
                return null
            }
            val tickets = number(frame,v,if(v.usesTallPhoneLayout).80 else .802,
                if(v.usesTallPhoneLayout).955 else .880,if(v.usesTallPhoneLayout).181 else .145)
            val matching = cyanButtons.filter { it.center.x in .35..0.65 && it.center.y > .70 }
                .maxByOrNull { it.width*it.height }
            if (matching != null) return DungeonPanel("network_matching", matching.center, tickets)
            val challenge = cyanButtons.filter { it.center.x > .50 && it.center.y in .48..0.68 && pairedPurple(it) }
                .maxByOrNull { it.width*it.height }
            if (challenge != null) return DungeonPanel("network_challenge", challenge.center, tickets)
        }
        val y = if (v.usesTallPhoneLayout) when(key) {
            DungeonKey.APOCALYMON_WALL -> .766
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .797
            DungeonKey.METAL_SEA -> .756
            else -> .712
        } else when(key) {
            DungeonKey.APOCALYMON_WALL -> .756
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .790
            DungeonKey.METAL_SEA -> .746
            else -> .700
        }
        val ticketY = if (v.usesTallPhoneLayout) when(key) {
            DungeonKey.APOCALYMON_WALL -> .721
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .181
            DungeonKey.METAL_SEA -> .700
            else -> .661
        } else when(key) {
            DungeonKey.APOCALYMON_WALL -> .708
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .145
            DungeonKey.METAL_SEA -> .690
            else -> .644
        }
        val ticketRange = if (v.usesTallPhoneLayout) when(key) {
            DungeonKey.NETWORK_DEFENSE, DungeonKey.DAILY -> .80 to .955
            DungeonKey.APOCALYMON_WALL -> .425 to .565
            else -> .41 to .58
        } else {
            val x = if (key == DungeonKey.NETWORK_DEFENSE || key == DungeonKey.DAILY) .802 else .480
            x to (x + .078)
        }
        val tickets = number(frame, v, ticketRange.first, ticketRange.second, ticketY)
        if (key == DungeonKey.DAILY && color(.41,y,true) > .45)
            return DungeonPanel("destroy", NormalizedPoint(.5,y), tickets)
        val adLeftX = if (v.usesTallPhoneLayout) .50 else .41
        if (color(adLeftX,y,true) > .45 && color(.58,y,true) > .45)
            return DungeonPanel("ad", NormalizedPoint(.5,y), number(frame,v,.514,.582,y))
        if (key == DungeonKey.NETWORK_DEFENSE) {
            return null
        } else if (color(.59,y) > .45) return DungeonPanel(
            "challenge",
            if (key == DungeonKey.APOCALYMON_WALL) NormalizedPoint(.50, y) else NormalizedPoint(.66, y),
            tickets,
        )
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
        // Only zero versus positive is safety-critical. Reuse the normalized glyph reader:
        // the old raw "has a hole" shortcut mistook the open triangle in the game's digit 4
        // for zero on high-density phone screenshots.
        return if (ShapeDigitReader.classify(mask, w, glyph) == 0) 0 else 1
    }
}

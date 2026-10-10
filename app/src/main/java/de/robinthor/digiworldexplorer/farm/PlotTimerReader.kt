package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*
import kotlin.math.abs

object PlotTimerReader {
    /** Growing-state proof does not require perfect OCR of every second. Locate the dark
     * timer plate and aligned pale digit row relative to the observed soil centre. */
    fun visible(frame:PixelFrame,viewport:GameViewport,center:NormalizedPoint):Boolean {
        val area=NormalizedRect((center.x-.20).coerceAtLeast(0.0),(center.y-.025).coerceAtLeast(0.0),
            (center.x+.20).coerceAtMost(1.0),(center.y+.12).coerceAtMost(1.0))
        return ColorRegionLocator.find(frame,viewport,area,(viewport.width/540).coerceAtLeast(1)) {
            val hsv=it.hsv();hsv.value<85 && hsv.saturation<150
        }.filter { it.width in .12.. .28 && it.height in .007.. .026 }.any { plate ->
            val dark=frame.ratioInViewportPatch(viewport,plate.center,plate.width*.4,plate.height*.3,1) { it.hsv().value<100 }
            val letters=frame.ratioInViewportPatch(viewport,plate.center,plate.width*.4,plate.height*.3,1) {
                val hsv=it.hsv();hsv.value>180 && hsv.saturation<80
            }
            dark>.40 && letters in .06.. .55
        }
    }
    /** Reads HH:MM:SS or MM:SS. Any incomplete/invalid row remains null. */
    fun read(frame: PixelFrame, plot: Int, viewport: GameViewport = GameViewport.detect(frame)): Int? {
        val center = FarmHarvestDetector.centers.getOrNull(plot) ?: return null
        val left = viewport.left + (viewport.width * (center.x - .15)).toInt()
        val right = viewport.left + (viewport.width * (center.x + .15)).toInt()
        val top = viewport.top + (viewport.height * (center.y - .02)).toInt()
        val bottom = viewport.top + (viewport.height * (center.y + .10)).toInt()
        if (right <= left || bottom <= top) return null
        val width = right - left
        val height = bottom - top
        val mask = BooleanArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val rgb = frame.rgbAt(left + x, top + y)
            mask[y * width + x] = .299 * rgb.red + .587 * rgb.green + .114 * rgb.blue >= 180
        }
        val components = ColorComponents.find(mask, width, height).filter { it.pixels >= 3 }
        val usedDots = mutableSetOf<ColorComponent>()
        val colons = mutableListOf<ColorComponent>()
        val candidates = components.sortedBy { it.left }
        for ((position, first) in candidates.withIndex()) {
            if (first in usedDots || first.height > 6 || first.width.toDouble() / first.height !in .5..2.0) continue
            val second = candidates.drop(position + 1).firstOrNull {
                it !in usedDots && it.height <= 6 && it.width.toDouble() / it.height in .5..2.0 &&
                    abs(it.left - first.left) <= maxOf(first.width, it.width) &&
                    abs(it.height - first.height) <= maxOf(first.height, it.height) * .6 &&
                    it.top > first.bottom && it.top - first.bottom <= (first.height + it.height) * 1.5
            } ?: continue
            usedDots += first; usedDots += second
            colons += ColorComponent(
                minOf(first.left, second.left), first.top,
                maxOf(first.right, second.right), second.bottom,
                first.pixels + second.pixels,
            )
        }
        if (colons.size !in 1..2) return null
        val digits = components.filter { it !in usedDots && it.height >= 6 }
        if (digits.size != (colons.size + 1) * 2) return null
        val maxHeight = digits.maxOf { it.height }
        if (digits.any { it.height < maxHeight * .85 }) return null
        val ordered = digits.sortedBy { it.left }
        // Every colon must sit between digit pairs, not elsewhere in the crop.
        val orderedColons = colons.sortedBy { it.left }
        if (orderedColons.indices.any { i -> orderedColons[i].left !in (ordered[i * 2 + 1].right + 1) until ordered[i * 2 + 2].left }) return null
        val values = ordered.map { ShapeDigitReader.classify(mask, width, it) ?: return null }
        val pairs = values.chunked(2).map { it[0] * 10 + it[1] }
        return if (colons.size == 2) {
            val (hours, minutes, seconds) = pairs
            if (hours !in 0..24 || minutes !in 0..59 || seconds !in 0..59) null
            else hours * 3600 + minutes * 60 + seconds
        } else {
            val (minutes, seconds) = pairs
            if (minutes !in 0..59 || seconds !in 0..59) null else minutes * 60 + seconds
        }
    }
}

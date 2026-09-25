package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*
import kotlin.math.abs

object PlotTimerReader {
    /** Reads HH:MM:SS or MM:SS. Any incomplete/invalid row remains null. */
    fun read(frame: PixelFrame, plot: Int, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): Int? {
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

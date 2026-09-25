package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*
import kotlin.math.abs

object FreeSeedCounterReader {
    /** Returns null for no row, more than two glyphs, noise or any unrecognized digit. */
    fun read(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): Int? {
        val left = viewport.left + (viewport.width * .375).toInt()
        val right = viewport.left + (viewport.width * .455).toInt()
        val top = viewport.top + (viewport.height * .06).toInt()
        val bottom = viewport.top + (viewport.height * .11).toInt()
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        val mask = BooleanArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val rgb = frame.rgbAt(left + x, top + y)
            mask[y * width + x] = .299 * rgb.red + .587 * rgb.green + .114 * rgb.blue >= 200
        }
        val components = ColorComponents.find(mask, width, height).filter { it.pixels >= 3 }
        if (components.isEmpty()) return null
        val rows = mutableListOf<MutableList<ColorComponent>>()
        for (component in components.sortedByDescending { it.height }) {
            val row = rows.firstOrNull {
                val anchor = it.first()
                abs(component.top - anchor.top) <= maxOf(2.0, anchor.height * .4) &&
                    abs(component.height - anchor.height) <= anchor.height / 3.0
            }
            if (row == null) rows += mutableListOf(component) else row += component
        }
        for (row in rows.sortedByDescending { it.size }) {
            val maxHeight = row.maxOf { it.height }
            val glyphs = row.filter { it.height >= maxHeight * .85 }.sortedBy { it.left }
            if (glyphs.size !in 1..2) continue
            var value = 0
            for (glyph in glyphs) {
                val digit = ShapeDigitReader.classify(mask, width, glyph) ?: return null
                value = value * 10 + digit
            }
            return value
        }
        return null
    }
}

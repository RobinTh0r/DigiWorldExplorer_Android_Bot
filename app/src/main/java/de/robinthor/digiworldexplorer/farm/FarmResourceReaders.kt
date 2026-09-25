package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*

/** Conservative HUD/dialog counters. Unknown glyphs stay null and never imply inventory. */
object FarmResourceReaders {
    private val hudSeedCenters = listOf(.418, .575, .725)

    fun hudSeeds(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): List<Int?> =
        hudSeedCenters.map { readNumber(frame, viewport, it - .04, it + .04, .055, .082) }

    fun wateringCans(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): Int? =
        readNumber(frame, viewport, .54, .61, .087, .105)

    fun dialogSeeds(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): List<Int?> =
        listOf(.306, .500, .694).map { readNumber(frame, viewport, it - .022, it + .022, .485, .501) }

    private fun readNumber(
        frame: PixelFrame,
        viewport: GameViewport,
        leftN: Double,
        rightN: Double,
        topN: Double,
        bottomN: Double,
    ): Int? {
        val left = viewport.left + (viewport.width * leftN).toInt()
        val right = viewport.left + (viewport.width * rightN).toInt()
        val top = viewport.top + (viewport.height * topN).toInt()
        val bottom = viewport.top + (viewport.height * bottomN).toInt()
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        val mask = BooleanArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val rgb = frame.rgbAt(left + x, top + y)
            mask[y * width + x] = .299 * rgb.red + .587 * rgb.green + .114 * rgb.blue >= 185
        }
        val components = ColorComponents.find(mask, width, height)
            .filter { it.pixels >= 3 && it.height >= 6 }
            .sortedBy { it.left }
        if (components.isEmpty()) return null
        val maxHeight = components.maxOf { it.height }
        val glyphs = components.filter { it.height >= maxHeight * .78 }.takeLast(2)
        if (glyphs.size !in 1..2) return null
        var result = 0
        for (glyph in glyphs) result = result * 10 + (ShapeDigitReader.classify(mask, width, glyph) ?: return null)
        return result
    }
}

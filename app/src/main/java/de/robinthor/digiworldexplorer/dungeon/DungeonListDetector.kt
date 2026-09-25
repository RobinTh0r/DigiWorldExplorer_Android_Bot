package de.robinthor.digiworldexplorer.dungeon

import de.robinthor.digiworldexplorer.vision.*

enum class DungeonListPosition { NONE, TOP, BOTTOM, MIDDLE }
enum class CounterAvailability { POSITIVE, ZERO, UNREADABLE }
data class DungeonCardReading(val key: DungeonKey, val center: NormalizedPoint, val tickets: CounterAvailability)
data class DungeonListReading(val position: DungeonListPosition, val cards: List<DungeonCardReading>)

/** Geometry-first reader for the seven fixed dungeon cards; translated text is never inspected. */
object DungeonListDetector {
    private const val W = 180
    private const val H = 320

    fun detect(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): DungeonListReading {
        val cyan = BooleanArray(W * H)
        for (y in 0 until H) for (x in 0 until W) {
            val hsv = frame.rgbAt(viewport.left + x * viewport.width / W, viewport.top + y * viewport.height / H).hsv()
            cyan[y * W + x] = hsv.hue in 90..115 && hsv.saturation >= 90 && hsv.value >= 115
        }
        val components = ColorComponents.find(cyan, W, H).filter {
            // Dungeon cards are at least about 9% of the viewport high. The shorter cyan
            // DUNGEON header can otherwise be mistaken for card zero at the bottom position,
            // shifting DAILY onto Metal Sea.
            it.pixels >= 75 && it.width.toDouble() / W in .66.. .86 && it.height.toDouble() / H in .09.. .24
        }.sortedBy { it.top }
        if (components.size < 3) return DungeonListReading(DungeonListPosition.NONE, emptyList())
        val firstHeight = components.first().height.toDouble() / H
        val firstCenter = (components.first().top + components.first().height / 2.0) / H
        val lastCenter = (components.last().top + components.last().height / 2.0) / H
        val position = when {
            firstHeight >= .17 && firstCenter < .34 -> DungeonListPosition.TOP
            firstHeight < .17 && components.size >= 4 && firstCenter < .30 && lastCenter > .72 -> DungeonListPosition.BOTTOM
            else -> DungeonListPosition.MIDDLE
        }
        val keys = when (position) {
            DungeonListPosition.TOP -> listOf(DungeonKey.APOCALYMON_WALL, DungeonKey.DEMIDEVIMON, DungeonKey.BAKEMON, DungeonKey.DIGIFACTORY, DungeonKey.NETWORK_DEFENSE)
            DungeonListPosition.BOTTOM -> listOf(DungeonKey.BAKEMON, DungeonKey.DIGIFACTORY, DungeonKey.NETWORK_DEFENSE, DungeonKey.METAL_SEA, DungeonKey.DAILY)
            else -> emptyList()
        }
        val cards = components.take(keys.size).mapIndexed { index, component ->
            val cx = (component.left + component.width / 2.0) / W
            val cy = (component.top + component.height / 2.0) / H
            val fh = component.height.toDouble() / H
            DungeonCardReading(keys[index], NormalizedPoint(cx, cy), readCounter(frame, viewport, cy, fh))
        }
        return DungeonListReading(position, cards)
    }

    /** Only zero versus positive is safety-critical; any ambiguous glyph stays unreadable. */
    private fun readCounter(frame: PixelFrame, viewport: GameViewport, cardCenterY: Double, cardHeight: Double): CounterAvailability {
        val x0 = viewport.left + (viewport.width * .10).toInt()
        val x1 = viewport.left + (viewport.width * .34).toInt()
        val bottom = cardCenterY + cardHeight / 2.0
        val y0 = viewport.top + (viewport.height * (bottom - .052)).toInt()
        val y1 = viewport.top + (viewport.height * (bottom - .006)).toInt()
        val width = (x1 - x0).coerceAtLeast(1); val height = (y1 - y0).coerceAtLeast(1)
        val mask = BooleanArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val rgb = frame.rgbAt(x0 + x, y0 + y)
            mask[y * width + x] = rgb.red >= 185 && rgb.green >= 185 && rgb.blue >= 185 &&
                maxOf(rgb.red, rgb.green, rgb.blue) - minOf(rgb.red, rgb.green, rgb.blue) <= 65
        }
        val glyphs = ColorComponents.find(mask, width, height).filter {
            it.height >= height * .20 && it.width.toDouble() / it.height in .15.. .86 && it.pixels >= 5
        }.sortedBy { it.left }
        if (glyphs.size < 2) return CounterAvailability.UNREADABLE
        val tallest = glyphs.maxBy { it.height }
        val others = glyphs.filter { it !== tallest }
        val median = others.map { it.height }.sorted()[others.size / 2]
        if (tallest.height < median * 1.10 || tallest.width.toDouble() / tallest.height > .62)
            return CounterAvailability.UNREADABLE
        val digits = glyphs.filter { it.right < tallest.left }
        val leading = digits.firstOrNull() ?: return CounterAvailability.UNREADABLE
        return if (hasEnclosedHole(mask, width, height, leading)) CounterAvailability.ZERO else CounterAvailability.POSITIVE
    }

    private fun hasEnclosedHole(mask: BooleanArray, width: Int, height: Int, glyph: ColorComponent): Boolean {
        val gw = glyph.width + 2; val gh = glyph.height + 2
        val background = BooleanArray(gw * gh) { true }
        for (y in 0 until glyph.height) for (x in 0 until glyph.width) {
            background[(y + 1) * gw + x + 1] = !mask[(glyph.top + y) * width + glyph.left + x]
        }
        val regions = ColorComponents.find(background, gw, gh)
        return regions.any { it.left > 0 && it.top > 0 && it.right < gw - 1 && it.bottom < gh - 1 && it.pixels >= 2 }
    }
}

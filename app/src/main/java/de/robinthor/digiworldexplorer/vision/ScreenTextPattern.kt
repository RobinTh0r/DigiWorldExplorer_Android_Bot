package de.robinthor.digiworldexplorer.vision

import kotlin.math.roundToInt

/** Small sampled text silhouette from our game screenshots, not a whole-screen template. */
class ScreenTextPattern(private val left: Int, private val top: Int, private val rows: List<String>) {
    fun matches(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): Boolean {
        return score(frame, viewport) >= .80
    }

    fun score(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): Double {
        var best = 0.0
        for (dy in -1..1) for (dx in -1..1) {
            var intersection = 0
            var union = 0
            for (y in rows.indices) for (x in rows[y].indices) {
                val p = frame.rgbAt(viewport.left + ((left + x * 3 + dx) * viewport.width / 720.0).roundToInt(),
                    viewport.top + ((top + y * 3 + dy) * viewport.height / 1280.0).roundToInt())
                val actual = p.red > 165 && p.green > 190 && p.blue > 190
                val expected = rows[y][x] == '#'
                if (actual && expected) intersection++
                if (actual || expected) union++
            }
            if (union > 30) best = maxOf(best, intersection.toDouble() / union)
        }
        return best
    }
}

package de.robinthor.digiworldexplorer.vision

/** Small sampled text silhouette from our game screenshots, not a whole-screen template. */
class ScreenTextPattern(private val left: Int, private val top: Int, private val rows: List<String>) {
    fun matches(frame: PixelFrame): Boolean {
        val viewport = GameViewport.fit(frame.width, frame.height)
        for (dy in -1..1) for (dx in -1..1) {
            var intersection = 0
            var union = 0
            for (y in rows.indices) for (x in rows[y].indices) {
                val p = frame.rgbAt(viewport.left + (left + x * 3 + dx) * viewport.width / 720,
                    viewport.top + (top + y * 3 + dy) * viewport.height / 1280)
                val actual = p.red > 165 && p.green > 190 && p.blue > 190
                val expected = rows[y][x] == '#'
                if (actual && expected) intersection++
                if (actual || expected) union++
            }
            if (union > 30 && intersection.toDouble() / union >= .80) return true
        }
        return false
    }
}

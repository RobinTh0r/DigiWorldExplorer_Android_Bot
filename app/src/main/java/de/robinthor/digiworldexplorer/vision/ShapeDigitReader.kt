package de.robinthor.digiworldexplorer.vision

/** Shape rules used by the game's compact white counters, normalized to a 12x16 bitmap. */
object ShapeDigitReader {
    private const val COLS = 12
    private const val ROWS = 16

    fun classify(source: BooleanArray, width: Int, component: ColorComponent): Int? {
        if (width <= 0 || source.size % width != 0 || component.width <= 0 || component.height <= 0) return null
        val height = source.size / width
        if (component.left < 0 || component.top < 0 || component.right >= width || component.bottom >= height) return null
        val bits = BooleanArray(COLS * ROWS)
        for (row in 0 until ROWS) for (col in 0 until COLS) {
            val x0 = component.left + col * component.width / COLS
            val x1 = component.left + ((col + 1) * component.width / COLS).coerceAtLeast(col * component.width / COLS + 1)
            val y0 = component.top + row * component.height / ROWS
            val y1 = component.top + ((row + 1) * component.height / ROWS).coerceAtLeast(row * component.height / ROWS + 1)
            var on = 0
            var total = 0
            for (y in y0 until y1.coerceAtMost(component.bottom + 1)) for (x in x0 until x1.coerceAtMost(component.right + 1)) {
                total++
                if (source[y * width + x]) on++
            }
            bits[row * COLS + col] = total > 0 && on * 2 >= total
        }
        val holes = holes(bits)
        val bottom = mean(bits, ROWS - 2, ROWS, 0, COLS)
        val right = mean(bits, 0, ROWS, COLS - 4, COLS)
        if (holes.size >= 2) return 8
        if (holes.size == 1) {
            if (bottom < .35) return 4
            if (holes[0] < .4) return 9
            if (holes[0] > .55) return 6
            return 0
        }
        if (bottom < .35) return 7
        if (right >= .25 && mean(bits, 2, 13, COLS - 3, COLS) >= .15) {
            // A small bold 3 can also have a solid bottom row. The lower stroke
            // of 2 runs down the left; 3 keeps its lower bowl on the right.
            if (bottom >= .85 && mean(bits, 9, 13, 0, 4) > mean(bits, 9, 13, COLS - 4, COLS)) return 2
            return if (mean(bits, 3, 7, 0, 4) > mean(bits, 3, 7, COLS - 4, COLS)) 5 else 3
        }
        return 1
    }

    private fun mean(bits: BooleanArray, r0: Int, r1: Int, c0: Int, c1: Int): Double {
        var count = 0
        for (r in r0 until r1) for (c in c0 until c1) if (bits[r * COLS + c]) count++
        return count.toDouble() / ((r1 - r0) * (c1 - c0)).coerceAtLeast(1)
    }

    /** Returns vertical centroids of enclosed background regions on a padded bitmap. */
    private fun holes(bits: BooleanArray): List<Double> {
        val paddedWidth = COLS + 2
        val paddedHeight = ROWS + 2
        val background = BooleanArray(paddedWidth * paddedHeight) { true }
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            background[(r + 1) * paddedWidth + c + 1] = !bits[r * COLS + c]
        }
        return ColorComponents.find(background, paddedWidth, paddedHeight).mapNotNull { component ->
            if (component.left == 0 || component.top == 0 || component.right == paddedWidth - 1 || component.bottom == paddedHeight - 1) null
            else {
                var rows = 0L
                var pixels = 0
                for (r in component.top..component.bottom) for (c in component.left..component.right) {
                    if (background[r * paddedWidth + c]) { rows += r; pixels++ }
                }
                if (pixels == 0) null else rows.toDouble() / pixels / paddedHeight
            }
        }
    }
}

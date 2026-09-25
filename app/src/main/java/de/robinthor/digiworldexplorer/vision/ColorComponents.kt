package de.robinthor.digiworldexplorer.vision

data class ColorComponent(val left: Int, val top: Int, val right: Int, val bottom: Int, val pixels: Int) {
    val width get() = right - left + 1
    val height get() = bottom - top + 1
}

/** Eight-connected flood fill; the input mask remains unchanged and allocation is bounded by it. */
object ColorComponents {
    fun find(mask: BooleanArray, width: Int, height: Int): List<ColorComponent> {
        require(width > 0 && height > 0 && width.toLong() * height == mask.size.toLong())
        val visited = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        val result = mutableListOf<ColorComponent>()
        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 1
            queue[0] = start
            visited[start] = true
            var left = start % width
            var right = left
            var top = start / width
            var bottom = top
            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                left = minOf(left, x); right = maxOf(right, x)
                top = minOf(top, y); bottom = maxOf(bottom, y)
                for (ny in maxOf(0, y - 1)..minOf(height - 1, y + 1)) {
                    for (nx in maxOf(0, x - 1)..minOf(width - 1, x + 1)) {
                        val adjacent = ny * width + nx
                        if (mask[adjacent] && !visited[adjacent]) {
                            visited[adjacent] = true
                            queue[tail++] = adjacent
                        }
                    }
                }
            }
            result += ColorComponent(left, top, right, bottom, tail)
        }
        return result
    }
}

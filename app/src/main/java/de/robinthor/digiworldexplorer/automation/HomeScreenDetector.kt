package de.robinthor.digiworldexplorer.automation

/** Language-independent home detector based on the beach, battle deck and right-side action rail. */
object HomeScreenDetector {
    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): Boolean {
        if (width < 1 || height < 1) return false
        val brightWorld = ratio(width, height, .12, .14, .88, .68, argbAt) { r, g, b ->
            (r + g + b) / 3 > 135
        }
        val warmWorld = ratio(width, height, .12, .14, .88, .68, argbAt) { r, g, b ->
            r > 120 && r > b * 1.12 && g > 75
        }
        val battleDeck = ratio(width, height, .05, .72, .95, .92, argbAt) { r, g, b ->
            b > 35 && b > r * 1.20 && r < 80
        }
        val centerNavigation = ratio(width, height, .40, .91, .60, .995, argbAt) { r, g, b ->
            b > 125 && g > 80 && b > r * 1.20
        }
        return (brightWorld >= .45 || warmWorld >= .25) && battleDeck >= .35 && centerNavigation >= .25
    }

    private inline fun ratio(
        width: Int, height: Int, x0: Double, y0: Double, x1: Double, y1: Double,
        argbAt: (Int, Int) -> Int,
        match: (Int, Int, Int) -> Boolean,
    ): Double {
        val step = (width / 240).coerceAtLeast(2)
        var hits = 0
        var total = 0
        for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
            for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                val value = argbAt(x, y)
                if (match(value shr 16 and 255, value shr 8 and 255, value and 255)) hits++
                total++
            }
        }
        return hits.toDouble() / total.coerceAtLeast(1)
    }
}

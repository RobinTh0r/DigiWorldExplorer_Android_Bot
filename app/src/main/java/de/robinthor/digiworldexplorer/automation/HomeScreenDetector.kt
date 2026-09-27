package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.GameViewport

/** Home-only controls, never the changing stage artwork or the shared currency header. */
object HomeScreenDetector {
    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): Boolean {
        return viewport(width, height, argbAt) != null
    }

    /** Return the geometry that actually matched, so Home taps use the same coordinates. */
    fun viewport(width: Int, height: Int, argbAt: (Int, Int) -> Int): GameViewport? {
        if (width < 1 || height < 1) return null
        val candidates = listOf(GameViewport(0, 0, width, height), GameViewport.fit(width, height)).distinct()
        return candidates.firstOrNull { bounds ->
            controlsMatch(bounds.width, bounds.height) { x, y -> argbAt(bounds.left + x, bounds.top + y) }
        }
    }

    private fun controlsMatch(width: Int, height: Int, argbAt: (Int, Int) -> Int): Boolean {
        // Yellow location pin with a blue center, paired with the Mission clipboard and
        // Login calendar. These three landmarks are independent of the stage artwork.
        val locationPin = ratio(width, height, .484, .105, .516, .124, argbAt) { r, g, b ->
            r > 190 && g > 170 && b < 140
        }
        val pinCenter = ratio(width, height, .493, .109, .507, .118, argbAt) { r, g, b ->
            r < 60 && b > 50 && b > g * 1.5
        }
        val missionPaper = ratio(width, height, .808, .132, .854, .154, argbAt) { r, g, b ->
            r > 170 && g > 190 && b > 180
        }
        val loginPaper = ratio(width, height, .802, .191, .858, .213, argbAt) { r, g, b ->
            r > 175 && g > 185 && b > 180
        }
        val homeIcons = locationPin > .12 && pinCenter > .12 && missionPaper > .25 && loginPaper > .25
        val battleDeck = ratio(width, height, .05, .72, .95, .92, argbAt) { r, g, b ->
            b > 35 && b > r * 1.20 && r < 80
        }
        val centerNavigation = ratio(width, height, .40, .91, .60, .995, argbAt) { r, g, b ->
            b > 125 && g > 80 && b > r * 1.20
        }
        // The battle background changes with the stage (including dark blue cities). The
        // gold level ring and cyan chamber below the battle remain fixed on Home.
        val levelRing = ratio(width, height, .40, .745, .59, .79, argbAt) { r, g, b ->
            r > 145 && g > 100 && b < g * .85
        }
        val chamber = ratio(width, height, .42, .79, .58, .86, argbAt) { r, g, b ->
            g > 120 && b > 160 && b > r * 1.25
        }
        val stableHomeControls = levelRing > .15 && chamber > .025
        return (homeIcons || (stableHomeControls && battleDeck >= .35)) && centerNavigation >= .25
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

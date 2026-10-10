package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.*

/** Small text silhouettes sampled from our own title screenshots, independent of seasonal art. */
object TitleScreenEvidence {
    private val touchStart = ScreenTextPattern(273, 1062, listOf(
        "..........................................................",
        "..........................................................",
        "....................#..................##.................",
        "..####..............#......####.......#..#.#...........#..",
        "...##..###.#..#..##.####....##..###...#...###.###..##.###.",
        "...##.#..#.#..#.#...#..#....##.#..#....###.#.....#.#...#..",
        "...##.#..#.#..#.#...#..#....##.#..#......#.#..#..#.#...#..",
        "...##..###.####.###.#..#....##.####...####.##.####.#...##.",
        "..........................................................",
        ".........................................................."))
    fun ready(frame: PixelFrame, viewport: GameViewport) = touchTarget(frame, viewport) != null

    fun touchTarget(frame: PixelFrame, viewport: GameViewport): NormalizedPoint? {
        if (touchStart.matches(frame, viewport)) return NormalizedPoint(.50, .843)
        return adaptiveTouchStartTarget(frame, viewport)
    }
    private val logo = ScreenTextPattern(459, 18, listOf(
        "............................................................",
        "..................##.......................##...............",
        ".#######..###...######..#.#.###....###..#######...##....##..",
        ".########.###..########.###.###...####.#########..####..##..",
        ".##...###..##.###....#..###.####.#####.###...###..#####.##..",
        ".##....##.###.###.......###.##########.###....###.########..",
        ".##....##.###.###..###..###.##########.##.....###.########..",
        "..#....##.###.###...###.###.##.#######.###...####.##.#####..",
        ".########.###..########.###.##..##.###.#########..##..####..",
        ".#######..###..########.###.##.....###..#####.#...##...###..",
        ".#####....###....####...###.##.....###...#####....##....##..",
        "............................................................",
        ".#########################################################..",
        "............................................................"))
    private val loading = ScreenTextPattern(288, 627, listOf(
        "....................................................................",
        "##...#.................#............................................",
        "###..#..###...#.#.##...#......###....###..#####.#..##..#..####......",
        "####.#.######.#.#.##...#.....######..###..#####.#..##..#..##.##.....",
        "####.#.#...##.######...#.....#...##..#.#..##.##.#..###.#..#...#.....",
        "######.#...##.#####....#.....#....#.##.##.##.##.#..#.#.#..#.###.....",
        "##.###.##..##.#####....#.....##..##.#####.##.##.#..#.###..#..##.....",
        "##.###..####...#.##....#####..####..#...#.#####.#..#..##..#####..##.",
        "....................................................................",
        "....................................................................",
        "...................................................................."))

    fun viewport(frame: PixelFrame): GameViewport? {
        // A bright Network battle also satisfies broad white publisher/logo ratios.
        // Its independently proven battle/challenge chrome vetoes every title fallback.
        if(de.robinthor.digiworldexplorer.network.NetworkDefenseScreenDetector.detect(
                frame.width,frame.height,frame::argbAt).screen !=
            de.robinthor.digiworldexplorer.network.NetworkDefenseScreen.NONE)return null
        val full = GameViewport(0, 0, frame.width, frame.height)
        if (adaptiveTitleChrome(frame, full)) return full
        return listOf(
        GameViewport(0, 0, frame.width, frame.height), GameViewport.detect(frame)
    ).distinct().map { it to maxOf(logo.score(frame, it), loading.score(frame, it)) }
        .filter { it.second >= .80 }.maxByOrNull { it.second }?.first
    }

    private fun adaptiveTitleChrome(frame: PixelFrame, v: GameViewport): Boolean {
        if (!v.usesTallPhoneLayout) return false
        val leftLogo = ratio(frame, v, .035, .045, .22, .115) { it.red > 210 && it.green > 210 && it.blue > 210 }
        val rightLogo = ratio(frame, v, .64, .045, .96, .105) { it.red > 190 && it.green > 190 && it.blue > 190 }
        if(leftLogo > .20 && rightLogo > .12)return true
        // Updated title artwork puts the white brand closer to the top. The overlay may hide
        // the left publisher logo, so require the independent central cyan/yellow game logo.
        val observedBrand=ratio(frame,v,.64,.005,.98,.065) { it.red>210 && it.green>210 && it.blue>210 }
        val centralCyan=ratio(frame,v,.15,.28,.90,.56) {
            val hsv=it.hsv();hsv.hue in 80..115 && hsv.saturation>60 && hsv.value>120
        }
        val centralYellow=ratio(frame,v,.32,.44,.71,.64) {
            val hsv=it.hsv();hsv.hue in 15..35 && hsv.saturation>100 && hsv.value>150
        }
        return observedBrand>.10 && centralCyan>.16 && centralYellow>.12
    }

    private fun adaptiveTouchStartTarget(frame: PixelFrame, v: GameViewport): NormalizedPoint? {
        if (!v.usesTallPhoneLayout || !adaptiveTitleChrome(frame, v)) return null
        // Phone renderers place the Touch-to-Start strip at different heights depending on
        // cutout/inset handling. Search the lower title area in narrow horizontal slices instead
        // of assuming the old 82.5–88.5% band, then tap the detected slice itself.
        var bestY = 0.0
        var bestScore = 0.0
        var y = .68
        while (y <= .93) {
            val cyan = ratio(frame, v, .12, y, .88, y + .035) {
                val h = it.hsv(); h.hue in 82..112 && h.saturation > 60 && h.value > 70
            }
            val letters = ratio(frame, v, .30, y + .004, .70, y + .031) {
                it.red > 170 && it.green > 185 && it.blue > 185
            }
            val score = cyan + letters * 4.0
            if (cyan > .10 && letters > .022 && score > bestScore) {
                bestScore = score
                bestY = y + .0175
            }
            y += .0125
        }
        return bestY.takeIf { it > 0.0 }?.let { NormalizedPoint(.50, it) }
    }

    private inline fun ratio(frame: PixelFrame, v: GameViewport, x0: Double, y0: Double, x1: Double, y1: Double,
        predicate: (Rgb) -> Boolean): Double {
        var hits = 0; var total = 0
        val step = (v.width / 240).coerceAtLeast(1)
        val left = v.left + (v.width * x0).toInt(); val right = v.left + (v.width * x1).toInt()
        val top = v.top + (v.height * y0).toInt(); val bottom = v.top + (v.height * y1).toInt()
        for (y in top until bottom step step) for (x in left until right step step) {
            if (predicate(frame.rgbAt(x, y))) hits++
            total++
        }
        return hits.toDouble() / total.coerceAtLeast(1)
    }
}

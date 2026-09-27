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
    fun ready(frame: PixelFrame, viewport: GameViewport) =
        touchStart.matches(frame, viewport) || adaptiveTouchStart(frame, viewport)
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
        val full = GameViewport(0, 0, frame.width, frame.height)
        if (adaptiveTitleChrome(frame, full)) return full
        return listOf(
        GameViewport(0, 0, frame.width, frame.height), GameViewport.fit(frame.width, frame.height)
    ).distinct().map { it to maxOf(logo.score(frame, it), loading.score(frame, it)) }
        .filter { it.second >= .80 }.maxByOrNull { it.second }?.first
    }

    private fun adaptiveTitleChrome(frame: PixelFrame, v: GameViewport): Boolean {
        if (!v.usesTallPhoneLayout) return false
        val leftLogo = ratio(frame, v, .035, .045, .22, .115) { it.red > 210 && it.green > 210 && it.blue > 210 }
        val rightLogo = ratio(frame, v, .64, .045, .96, .105) { it.red > 190 && it.green > 190 && it.blue > 190 }
        return leftLogo > .20 && rightLogo > .12
    }

    private fun adaptiveTouchStart(frame: PixelFrame, v: GameViewport): Boolean {
        if (!v.usesTallPhoneLayout || !adaptiveTitleChrome(frame, v)) return false
        val band = ratio(frame, v, .12, .825, .88, .885) {
            val h = it.hsv(); h.hue in 85..110 && h.saturation > 70 && h.value > 80
        }
        val letters = ratio(frame, v, .32, .835, .68, .865) {
            it.red > 175 && it.green > 190 && it.blue > 190
        }
        return band > .18 && letters > .035
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

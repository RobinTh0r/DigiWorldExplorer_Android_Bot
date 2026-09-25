package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.*

enum class IdleRewardScreen { NONE, CLAIM, EMPTY, RESULT }

/** Only screenshot-proven German idle screens. Other languages remain unknown, never guessed. */
object IdleRewardDetector {
    private val title = ScreenTextPattern(240, 268, listOf(
        "..........................................................................................",
        "...#......#..#............#####........#........#.........................................",
        "...#......#..#............#...#........#........#.........................................",
        "...#...####..#..###.......#...#...###..#...###..#.##..#####..#..#..#.##...####............",
        "...#..#...#..#.#...#......####...#..##.#..#..##.##.##.##..#..#..#..#..#..#..##............",
        "...#..#...#..#.#####.###..#...#..#####.#..#...#.#..##.##..#..#..#..#..#..##.#.............",
        "...#..#...#..#.#..........#...##.#.....#..#..##.#..##.##..#..#..#..#..#...#...............",
        "...#..#####..#..####......#####...####.#..####..#..##.##..#..####..#..#..####.............",
        ".........................................................................#...#............"))
    private val dismiss = ScreenTextPattern(243, 1034, listOf(
        "....................................................................................",
        "..................##.....#...#.......##.....................#.......................",
        "....#............#.......#...#......#.#...................#.........................",
        "...#....#.###.#..##..##..#.#.#..#.#.#.#.#.###....#..##..#.#.###.###.#.#.###.......",
        "..#.....#...#.#....#.#...#.#.#..###.#...###.#.#...##....#.#.#.#...#...###.#.#.......",
        ".###..###...#.#..###.###.#.#.#...#..#.#.##..#.#...##....#.#.#.#.#.#.#.##..#.#.......",
        "..............................................................#...#.................",
        "..............................................................#...#.................",
        "...................................................................................."))

    fun detect(frame: PixelFrame): IdleRewardScreen {
        val v = GameViewport.fit(frame.width, frame.height)
        fun blue(x: Double, y: Double) = frame.ratioInViewportPatch(v, NormalizedPoint(x,y), .012, .015) {
            it.blue > 100 && it.blue > it.red * 2 && it.blue > it.green
        } > .8
        // Specific modal side panels, not just a shared cyan game button.
        if (!blue(.195,.55) || !blue(.80,.55)) return IdleRewardScreen.NONE
        if (dismiss.matches(frame)) return IdleRewardScreen.RESULT
        if (!title.matches(frame)) return IdleRewardScreen.NONE
        val claimEnabled = frame.ratioInViewportPatch(v, NormalizedPoint(.632,.72), .08,.006) {
            it.red < 40 && it.green > 110 && it.blue > 180
        } > .85
        return if (claimEnabled) IdleRewardScreen.CLAIM else IdleRewardScreen.EMPTY
    }
}

package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*

data class PartnerGrid(val page: Boolean = false, val expanded: Boolean = false,
    val cells: List<NormalizedPoint> = emptyList(), val raised: Int? = null,
    val selected: Int? = null, val canRaise: Boolean = false, val confirmation: Boolean = false,
    val raiseTarget: NormalizedPoint? = null)

/** Independent screen evidence for the visible 5x3 Partner roster. */
object PartnerGridDetector {
    fun detect(frame: PixelFrame): PartnerGrid {
        val viewport = GameViewport.fit(frame.width, frame.height)
        fun ratio(x: Double, y: Double, rx: Double, ry: Double, match: (Hsv) -> Boolean) =
            frame.ratioInViewportPatch(viewport, NormalizedPoint(x, y), rx, ry, 1) { match(it.hsv()) }
        fun cyan(x: Double, y: Double, rx: Double, ry: Double) = ratio(x, y, rx, ry) {
            it.hue in 85..110 && it.saturation > 120 && it.value > 170
        }
        val header = cyan(.5, .09, .36, .012) > .45
        val hero = cyan(.13, .285, .004, .14) > .30 && cyan(.87, .285, .004, .14) > .30 &&
            cyan(.5, .4515, .34, .001) > .55
        // A dimmed page is accepted only as a prompt shape; its caller must own a Raise.
        val prompt = ratio(.35, .59, .075, .013) { it.hue in 135..165 && it.saturation > 100 && it.value > 160 } > .45 &&
            cyan(.635, .59, .075, .013) > .45
        if (!header || !hero) return PartnerGrid(confirmation = prompt)
        val expanded = cyan(.159, .642, .003, .025) > .45
        if (!expanded) return PartnerGrid(page = true)
        val cells = (0 until 15).map { i -> NormalizedPoint(.214 + (i % 5) * .143, .644 + (i / 5) * .0825) }
        // Each cell must have its blue/cyan left frame; no guessed empty or off-screen cell taps.
        if (cells.count { cyan(it.x - .055, it.y, .003, .024) > .30 } < 14)
            return PartnerGrid(page = true)
        val raised = cells.indices.filter { i ->
            val p = cells[i]
            ratio(p.x - .039, p.y - .021, .012, .008) { it.hue in 35..80 && it.saturation > 140 && it.value > 160 } > .12 &&
                ratio(p.x - .039, p.y - .021, .012, .008) { it.value < 130 } > .45
        }.singleOrNull()
        val selected = cells.indices.filter { i ->
            val p = cells[i]
            ratio(p.x - .065, p.y - .025, .004, .016) { it.hue in 20..35 && it.saturation > 150 && it.value > 190 } > .35
        }.singleOrNull()
        // MAX-level partners show two buttons. Only the cyan Start button is actionable.
        val raiseTarget = listOf(.5, .632).firstOrNull { cyan(it, .397, .085, .012) > .65 }
            ?.let { NormalizedPoint(it, .4) }
        return PartnerGrid(true, true, cells, raised, selected, raiseTarget != null, prompt, raiseTarget)
    }
}

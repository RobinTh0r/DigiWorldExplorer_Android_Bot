package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*

data class PartnerGrid(val page: Boolean = false, val expanded: Boolean = false,
    val cells: List<NormalizedPoint> = emptyList(), val raised: Int? = null,
    val selected: Int? = null, val canRaise: Boolean = false, val confirmation: Boolean = false,
    val raiseTarget: NormalizedPoint? = null, val digimonSection: Boolean = false,
    val partnerTabTarget: NormalizedPoint? = null,
    val confirmationTarget: NormalizedPoint? = null,
    val expandTarget: NormalizedPoint? = null)

/** Independent screen evidence for the visible 5x3 Partner roster. */
object PartnerGridDetector {
    fun detect(frame: PixelFrame): PartnerGrid {
        val viewport = GameViewport.detect(frame)
        val tallPhone = viewport.usesTallPhoneLayout
        fun ratio(x: Double, y: Double, rx: Double, ry: Double, match: (Hsv) -> Boolean) =
            frame.ratioInViewportPatch(viewport, NormalizedPoint(x, y), rx, ry, 1) { match(it.hsv()) }
        fun cyan(x: Double, y: Double, rx: Double, ry: Double) = ratio(x, y, rx, ry) {
            it.hue in 85..110 && it.saturation > 120 && it.value > 170
        }
        fun blue(x: Double, y: Double, rx: Double, ry: Double) = ratio(x, y, rx, ry) {
            it.hue in 80..120 && it.saturation > 75 && it.value > 95
        }
        fun pale(x: Double, y: Double, rx: Double, ry: Double) = ratio(x, y, rx, ry) {
            it.value > 190 && it.saturation < 130
        }
        val headerY = if (tallPhone) .128 else .09
        val heroLeft = if (tallPhone) .058 else .13
        val heroRight = if (tallPhone) .944 else .87
        val heroY = if (tallPhone) .318 else .285
        val heroHalfHeight = if (tallPhone) .145 else .14
        val heroBottom = if (tallPhone) .472 else .4515
        val header = cyan(.5, headerY, .36, .012) > .45
        // The selected Digimon bottom-navigation button has a large white mascot tile. Home,
        // Explore and Dungeon keep this area blue, which prevents their cyan headers from being
        // mistaken for a Buddy/Support/Partner subpage.
        val digimonNavSelected = pale(.26, .94, .07, .035) > .12
        val digimonSection = header && digimonNavSelected
        val geometry = if (digimonSection) PartnerVisualLocator.detect(frame, viewport) else null
        val heroSides = cyan(heroLeft, heroY, .004, heroHalfHeight) > .30 &&
            cyan(heroRight, heroY, .004, heroHalfHeight) > .30
        val hero = geometry != null || (heroSides && (tallPhone || cyan(.5, heroBottom, .42, .001) > .55))
        // A dimmed page is accepted only as a prompt shape; its caller must own a Raise.
        val confirmationTarget = PartnerVisualLocator.confirmationTarget(frame, viewport)
        val prompt = confirmationTarget != null
        // Buddy, Support Digimon and Partner share the same full-width cyan Digimon header.
        // Bond owns this recovery only after it deliberately opened the Digimon section from
        // verified Home, so the shared header is safe evidence for selecting the Partner tab.
        val partnerTab = NormalizedPoint(if (tallPhone) .115 else .125, .875)
        if (!digimonSection || !hero) return PartnerGrid(
            confirmation = prompt,
            confirmationTarget = confirmationTarget,
            digimonSection = digimonSection,
            partnerTabTarget = partnerTab.takeIf { digimonSection },
        )
        val expanded = geometry?.cells?.isNotEmpty() == true || if (tallPhone) {
            // The adaptive phone layout keeps the roster's first-row position occupied by
            // passive-skill icons while collapsed, so a blue-frame probe is ambiguous. The
            // bottom-right control is unambiguous: '+' has a bright vertical stroke, '−' has not.
            pale(.883, .827, .006, .018) < .30 && pale(.883, .827, .018, .006) > .30
        } else cyan(.159, .642, .003, .025) > .45
        if (!expanded) return PartnerGrid(
            page = true,
            digimonSection = true,
            partnerTabTarget = partnerTab,
            expandTarget = PartnerVisualLocator.expandTarget(frame, viewport),
        )
        val cells = geometry?.cells?.takeIf { it.isNotEmpty() } ?: (0 until 15).map { i ->
            // Real 1080x2376 OnePlus captures use a tighter roster than the earlier Oppo
            // fixture: row centres are roughly 66.0%, 73.5% and 81.0% of the game viewport.
            if (tallPhone) NormalizedPoint(.158 + (i % 5) * .172, .660 + (i / 5) * .075)
            else NormalizedPoint(.214 + (i % 5) * .143, .644 + (i / 5) * .0825)
        }
        // Each cell must have its blue/cyan left frame; no guessed empty or off-screen cell taps.
        val cellFrameOffset = if (tallPhone) .067 else .055
        val provedCells = if (tallPhone) cells.count { blue(it.x - cellFrameOffset, it.y, .006, .027) > .25 }
            else cells.count { cyan(it.x - cellFrameOffset, it.y, .003, .024) > .30 }
        if (geometry?.cells?.isNotEmpty() != true && provedCells < 14)
            return PartnerGrid(page = true, digimonSection = true, partnerTabTarget = partnerTab,
                expandTarget = PartnerVisualLocator.expandTarget(frame, viewport))
        val raised = if (tallPhone) {
            // Read the small dark check badge, not a broad portrait area. Green parts of a
            // Digimon used to compete with the real marker and could leave `raised` ambiguous.
            val scores = cells.indices.map { i ->
                val p=cells[i]
                val green = ratio(p.x-.044,p.y-.022,.020,.015) {
                    it.hue in 35..80 && it.saturation>125 && it.value>135
                }
                val dark = ratio(p.x-.044,p.y-.022,.020,.015) { it.value < 125 }
                i to if (dark > .25) green else 0.0
            }.sortedByDescending { it.second }
            scores.firstOrNull()?.takeIf { best ->
                best.second > .035 && best.second-(scores.getOrNull(1)?.second ?: 0.0) > .015
            }?.first
        } else cells.indices.filter { i ->
            val p = cells[i]
                ratio(p.x - .039, p.y - .021, .012, .008) { it.hue in 35..80 && it.saturation > 140 && it.value > 160 } > .12 &&
                    ratio(p.x - .039, p.y - .021, .012, .008) { it.value < 130 } > .45
        }.singleOrNull()
        // The yellow frame is animated and disconnected at its corners. Measure several edges
        // around each observed portrait, rather than requiring one exact left-edge pixel strip.
        val visualSelected = if (geometry?.cells?.isNotEmpty() == true) cells.indices.filter { i ->
            val p = cells[i]
            val cw = geometry.cellWidth
            val ch = cw * viewport.width / viewport.height
            fun yellow(x: Double, y: Double, rx: Double, ry: Double) = ratio(x, y, rx, ry) {
                it.hue in 20..35 && it.saturation > 120 && it.value > 185
            }
            listOf(
                yellow(p.x, p.y - ch / 2 - .006, cw * .40, .007),
                yellow(p.x, p.y + ch / 2 + .006, cw * .40, .007),
                yellow(p.x - cw / 2 - .008, p.y, .009, ch * .40),
                yellow(p.x + cw / 2 + .008, p.y, .009, ch * .40),
            ).count { it > .08 } >= 2
        }.singleOrNull() else null
        val selected = visualSelected ?: cells.indices.filter { i ->
            val p = cells[i]
            ratio(p.x - if (tallPhone) .079 else .065, p.y - if (tallPhone) .012 else .025, .004, if (tallPhone) .026 else .016) {
                it.hue in 20..35 && it.saturation > 150 && it.value > 190
            } > if (tallPhone) .18 else .35
        }.singleOrNull()
        // MAX-level partners show two buttons. Only the cyan Start button is actionable.
        val raiseY = if (tallPhone) .425 else .397
        val raiseTarget = geometry?.raiseTarget ?: listOf(.5, .632).firstOrNull { cyan(it, raiseY, .085, .012) > .65 }
            ?.let { NormalizedPoint(it, raiseY + .003) }
        return PartnerGrid(true, true, cells, raised, selected, raiseTarget != null, prompt, raiseTarget,
            digimonSection = true, partnerTabTarget = partnerTab, confirmationTarget = confirmationTarget)
    }
}

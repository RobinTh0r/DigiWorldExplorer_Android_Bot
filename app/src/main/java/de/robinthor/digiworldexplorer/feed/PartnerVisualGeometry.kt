package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*
import kotlin.math.abs

/** Geometry measured from the current image, independent of device names and portrait artwork. */
internal data class PartnerVisualGeometry(
    val hero: NormalizedRect,
    val cells: List<NormalizedPoint>,
    val cellWidth: Double = 0.0,
    val raiseTarget: NormalizedPoint? = null,
)

internal object PartnerVisualLocator {
    fun expandTarget(frame: PixelFrame, viewport: GameViewport): NormalizedPoint? {
        return ColorRegionLocator.find(frame, viewport, NormalizedRect(.74, .76, .95, .86),
            (viewport.width / 540).coerceAtLeast(1)) {
            val hsv = it.hsv(); hsv.value > 205 && hsv.saturation < 65
        }.filter { region ->
            val pixelAspect = region.width * viewport.width / (region.height * viewport.height)
            region.width in .022.. .055 && pixelAspect in .75..1.30
        }.filter { region ->
            fun white(dx: Double, dy: Double) = frame.ratioInViewportPatch(viewport,
                NormalizedPoint(region.center.x + dx * region.width, region.center.y + dy * region.height),
                region.width * .08, region.height * .08, 1) {
                val hsv = it.hsv(); hsv.value > 205 && hsv.saturation < 65
            } > .6
            // A cross has four bright arms and dark corners, unlike text or a portrait highlight.
            white(0.0, 0.0) && white(-.3, 0.0) && white(.3, 0.0) &&
                white(0.0, -.3) && white(0.0, .3) &&
                !white(-.35, -.35) && !white(.35, .35)
        }.singleOrNull()?.center
    }

    /** Both aligned buttons are required; a single cyan game button is not a dialog. */
    fun confirmationTarget(frame: PixelFrame, viewport: GameViewport): NormalizedPoint? {
        fun buttons(purple: Boolean) = ColorRegionLocator.find(frame, viewport,
            NormalizedRect(.16, .46, .84, .72), (viewport.width / 540).coerceAtLeast(1)) {
            val hsv = it.hsv()
            hsv.hue in (if (purple) 135..165 else 85..110) && hsv.saturation > 100 && hsv.value > 160
        }.filter { it.width in .15.. .30 && it.height in .025.. .065 }.filter { region ->
            frame.ratioInViewportPatch(viewport, region.center, region.width * .4, region.height * .35, 1) {
                val hsv = it.hsv()
                hsv.hue in (if (purple) 135..165 else 85..110) && hsv.saturation > 100 && hsv.value > 160
            } > .45
        }
        val cancel = buttons(true)
        return buttons(false).filter { yes -> cancel.any { no ->
            no.right < yes.left && yes.left - no.right in .025.. .15 &&
                abs(no.center.y - yes.center.y) < .01 && abs(no.width - yes.width) < .035 &&
                abs(no.height - yes.height) < .015
        } }.singleOrNull()?.center
    }

    fun detect(frame: PixelFrame, viewport: GameViewport): PartnerVisualGeometry? {
        val regions = ColorRegionLocator.find(frame, viewport, NormalizedRect(.025, .11, .975, .86),
            sampleStep = (viewport.width / 540).coerceAtLeast(1)) {
            val hsv = it.hsv()
            hsv.hue in 85..110 && hsv.saturation > 100 && hsv.value > 130
        }
        val hero = regions.filter {
            it.top < .24 && it.bottom in .40.. .55 && it.width in .65.. .94 && it.height > .23
        }.maxByOrNull { it.width } ?: return null
        val raise = regions.filter {
            it.top > hero.top + hero.height * .65 && it.bottom < hero.bottom &&
                it.width in .17.. .40 && it.height in .025.. .065
        }.filter { region ->
            frame.ratioInViewportPatch(viewport, region.center, region.width * .3, region.height * .2, 1) {
                val hsv = it.hsv(); hsv.hue in 85..110 && hsv.saturation > 120 && hsv.value > 170
            } > .60
        }.maxByOrNull { it.width }?.center
        val roster = regions.filter {
            it.top in .55.. .68 && it.bottom > .79 && it.width > hero.width * .85 && it.height > .18
        }.maxByOrNull { it.width } ?: return PartnerVisualGeometry(hero, emptyList(), raiseTarget = raise)
        // Portraits may touch their top borders, but their cyan lower/right borders remain stable.
        // Fit the repeated five-column lattice to those observed edges, then verify every cell.
        val cards = regions.filter {
            it.left >= hero.left && it.right <= hero.right + .01 && it.top >= roster.top &&
                it.width in .09.. .20 && it.height in .04.. .10
        }
        fun groups(values: List<Double>, tolerance: Double): List<List<Double>> {
            val result = mutableListOf<MutableList<Double>>()
            for (value in values.sorted()) {
                val last = result.lastOrNull()
                if (last != null && abs(value - last.average()) < tolerance) last += value
                else result += mutableListOf(value)
            }
            return result
        }
        fun median(values: List<Double>) = values.sorted()[(values.size - 1) / 2]
        val rows = groups(cards.map { it.bottom }, .014).filter { it.size >= 4 }
        val columns = groups(cards.map { it.right }, .025).filter { it.size >= 2 }
        if (rows.size < 2 || columns.size != 5) return PartnerVisualGeometry(hero, emptyList())
        val rowEnds = rows.take(2).map(::median)
        val spacing = rowEnds[1] - rowEnds[0]
        val rightEdges = columns.map(::median)
        val columnSpacing = median(rightEdges.zipWithNext { a, b -> b - a })
        if (spacing !in .06.. .10 || columnSpacing !in .12.. .19 ||
            rightEdges.zipWithNext { a, b -> abs((b - a) - columnSpacing) }.any { it > .02 })
            return PartnerVisualGeometry(hero, emptyList())
        val cardWidth = median(cards.map { it.width })
        val cardHeight = cardWidth * viewport.width / viewport.height
        val cells = (0 until 15).map { index ->
            NormalizedPoint(rightEdges[index % 5] - cardWidth / 2,
                rowEnds[0] + spacing * (index / 5) - cardHeight / 2)
        }
        val verified = cells.all { cell ->
            // A predicted lattice location alone never authorizes a tap. Require its own border.
            listOf(-.5, .5).any { side ->
                frame.ratioInViewportPatch(viewport,
                    NormalizedPoint(cell.x + side * cardWidth, cell.y), .008, cardHeight * .3, 1) {
                    val hsv = it.hsv(); hsv.hue in 80..120 && hsv.saturation > 75 && hsv.value > 95
                } > .18
            }
        }
        return PartnerVisualGeometry(hero, if (verified) cells else emptyList(), cardWidth, raise)
    }
}

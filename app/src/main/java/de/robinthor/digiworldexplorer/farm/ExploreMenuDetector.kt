package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.ColorComponent
import de.robinthor.digiworldexplorer.vision.ColorComponents
import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame
import de.robinthor.digiworldexplorer.vision.ratioInViewportPatch
import kotlin.math.abs
import kotlin.math.hypot

data class ExploreMenuDetection(
    val menu: Boolean = false,
    val worldSearchTarget: NormalizedPoint? = null,
    val meatFieldTarget: NormalizedPoint? = null,
)

/** Independent normalized-port of the Explore-card geometry; it never taps on color alone. */
object ExploreMenuDetector {
    private const val W = 360
    private const val H = 640

    fun detect(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): ExploreMenuDetection {
        if (de.robinthor.digiworldexplorer.feed.PartnerGridDetector.detect(frame).page) return ExploreMenuDetection()
        val dark = BooleanArray(W * H)
        val world = BooleanArray(W * H)
        val field = BooleanArray(W * H)
        for (y in 0 until H) for (x in 0 until W) {
            val hsv = frame.rgbAt(
                viewport.left + x * viewport.width / W,
                viewport.top + y * viewport.height / H,
            ).hsv()
            val i = y * W + x
            dark[i] = hsv.hue in 95..130 && hsv.saturation >= 120 && hsv.value in 20..150
            world[i] = hsv.hue in 140..160 && hsv.saturation >= 90 && hsv.value >= 120
            field[i] = hsv.hue in 35..55 && hsv.saturation >= 60 && hsv.value >= 60
        }
        var darkCount = 0
        var bandCount = 0
        for (y in (H * .16).toInt() until (H * .86).toInt()) for (x in 0 until W) {
            bandCount++
            if (dark[y * W + x]) darkCount++
        }
        if (darkCount.toDouble() / bandCount.coerceAtLeast(1) < .45) return ExploreMenuDetection()
        // Restrict both anchors to their own card. Other coloured cards and the
        // draggable overlay must not compete with a known card's component.
        for (y in 0 until H) for (x in 0 until W) {
            if (x.toDouble() / W !in .18.. .43 || y.toDouble() / H !in .16.. .29) world[y * W + x] = false
            if (x.toDouble() / W !in .20.. .47 || y.toDouble() / H !in .38.. .50) field[y * W + x] = false
        }
        val worldBlob = dominant(world, .001)
        val fieldBlob = dominant(field, .001)
        if (worldBlob == null) {
            // The status bubble can cover the upper card. Require the cyan page
            // header in addition to the field illustration in that case.
            val header = frame.ratioInViewportPatch(viewport,
                NormalizedPoint(.5, .09), .35, .012) {
                it.blue > 170 && it.green > 100 && it.red < 130
            }
            return ExploreMenuDetection(menu = true, meatFieldTarget =
                fieldBlob?.center()?.takeIf { header > .5 }?.copy(y = .52))
        }
        val worldCenter = worldBlob.center()
        if (abs(worldCenter.x - .294) > .06 || abs(worldCenter.y - .214) > .06)
            return ExploreMenuDetection(menu = true)
        val worldTarget = worldCenter.copy(y = worldCenter.y + .072)
        if (fieldBlob == null) return ExploreMenuDetection(menu = true, worldSearchTarget = worldTarget)
        val fieldCenter = fieldBlob.center()
        val fieldTarget = fieldCenter.copy(y = fieldCenter.y + .073)
        val anchored = abs((fieldTarget.x - worldTarget.x) - .0715) <= .03 &&
            abs((fieldTarget.y - worldTarget.y) - .2442) <= .03
        return ExploreMenuDetection(true, worldTarget, fieldTarget.takeIf { anchored })
    }

    private fun dominant(mask: BooleanArray, minShare: Double): ColorComponent? {
        val candidates = ColorComponents.find(mask, W, H).filter {
            it.pixels.toDouble() / mask.size >= minShare && it.center().y in .16.. .86
        }
        val biggest = candidates.maxByOrNull { it.pixels } ?: return null
        val foreign = candidates.filter { it !== biggest && hypot(it.center().x - biggest.center().x, it.center().y - biggest.center().y) >= .17 }
            .maxByOrNull { it.pixels }
        if (foreign != null && biggest.pixels < foreign.pixels * 3) return null
        return biggest
    }

    private fun ColorComponent.center() = NormalizedPoint(
        (left + width / 2.0) / W,
        (top + height / 2.0) / H,
    )
}

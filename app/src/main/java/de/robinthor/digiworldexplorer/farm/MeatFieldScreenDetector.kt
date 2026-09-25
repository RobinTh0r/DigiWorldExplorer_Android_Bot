package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame
import de.robinthor.digiworldexplorer.vision.ratioInViewportPatch

data class FarmPlot(
    val column: Int,
    val row: Int,
    val center: NormalizedPoint,
    val tapTarget: NormalizedPoint,
    val groundRatio: Double,
)

data class MeatFieldDetection(
    val recognized: Boolean,
    val plots: List<FarmPlot>,
    val confidence: Double,
)

/**
 * Conservative first-stage detector for the 2x3 Meat Field lattice.
 *
 * It intentionally does not label plots ripe/empty/growing yet: those actions remain disabled until
 * real screenshots cover badges, timers, seed selection and the Water popup.
 */
object MeatFieldScreenDetector {
    private val columns = doubleArrayOf(.313, .640)
    private val rows = doubleArrayOf(.507, .653, .810)

    fun detect(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): MeatFieldDetection {
        val plots = buildList {
            rows.forEachIndexed { row, y ->
                columns.forEachIndexed { column, x ->
                    val center = NormalizedPoint(x, y)
                    val ratio = frame.ratioInViewportPatch(viewport, center, .035, .022, step = 2) { rgb ->
                        val hsv = rgb.hsv()
                        hsv.hue in 0..20 && hsv.saturation >= 90 && hsv.value >= 40
                    }
                    val tapX = x + if (column == 0) .070 else -.070
                    add(FarmPlot(column, row, center, NormalizedPoint(tapX, y), ratio))
                }
            }
        }
        val matched = plots.count { it.groundRatio >= MIN_GROUND_RATIO }
        val confidence = plots.map { (it.groundRatio / TARGET_GROUND_RATIO).coerceIn(0.0, 1.0) }.average()
        return MeatFieldDetection(matched == plots.size && confidence >= MIN_CONFIDENCE, plots, confidence)
    }

    private const val MIN_GROUND_RATIO = .18
    private const val TARGET_GROUND_RATIO = .45
    private const val MIN_CONFIDENCE = .55
}

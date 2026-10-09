package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.ColorRegionLocator
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.NormalizedRect
import de.robinthor.digiworldexplorer.vision.PixelFrame
import de.robinthor.digiworldexplorer.vision.ratioInViewportPatch

data class HomeIdleRewardReading(
    val available: Boolean,
    val target: NormalizedPoint? = null,
    val greenRatio: Double = 0.0,
    val cyanRatio: Double = 0.0,
    val chestVisible: Boolean = false,
)

/** Detects the green reward marker attached to the open box on a positively verified Home. */
object HomeIdleRewardDetector {
    fun detect(frame: PixelFrame, homeAlreadyConfirmed: Boolean = false): HomeIdleRewardReading {
        val viewport = HomeScreenDetector.viewport(frame.width, frame.height, frame::argbAt)
            ?: if (homeAlreadyConfirmed) GameViewport.detect(frame)
            else return HomeIdleRewardReading(false)
        val chest = ColorRegionLocator.find(frame, viewport, NormalizedRect(.04, .54, .27, .80),
            (viewport.width / 540).coerceAtLeast(1)) {
            val hsv = it.hsv(); hsv.hue in 82..110 && hsv.saturation > 75 && hsv.value > 120
        }.filter { it.width in .07.. .15 && it.height in .035.. .09 &&
            it.top in .56.. .74 && it.bottom in .61.. .80
        }.maxByOrNull { it.width } ?: return HomeIdleRewardReading(false)
        val evidenceCenter = NormalizedPoint(chest.center.x, chest.top - chest.height * .10)
        val greenRatio = frame.ratioInViewportPatch(viewport, evidenceCenter,
            chest.width * .72, chest.height * .85, 1) {
            val hsv = it.hsv(); hsv.hue in 28..62 && hsv.saturation >= 90 && hsv.value >= 135
        }
        val cyanRatio = frame.ratioInViewportPatch(viewport, chest.center,
            chest.width * .46, chest.height * .46, 1) {
            val hsv = it.hsv(); hsv.hue in 82..105 && hsv.saturation >= 75 && hsv.value >= 125
        }
        val available = greenRatio >= .012 && cyanRatio >= .055
        return HomeIdleRewardReading(available, chest.center.takeIf { available },
            greenRatio, cyanRatio, chestVisible = true)
    }
}

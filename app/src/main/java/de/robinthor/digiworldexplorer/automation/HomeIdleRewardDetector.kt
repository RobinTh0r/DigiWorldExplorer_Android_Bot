package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame

data class HomeIdleRewardReading(
    val available: Boolean,
    val target: NormalizedPoint? = null,
    val greenRatio: Double = 0.0,
    val cyanRatio: Double = 0.0,
)

/** Detects the green reward marker attached to the open box on a positively verified Home. */
object HomeIdleRewardDetector {
    fun detect(frame: PixelFrame, homeAlreadyConfirmed: Boolean = false): HomeIdleRewardReading {
        val viewport = HomeScreenDetector.viewport(frame.width, frame.height, frame::argbAt)
            ?: if (homeAlreadyConfirmed) GameViewport.fit(frame.width, frame.height)
            else return HomeIdleRewardReading(false)
        var green = 0
        var cyan = 0
        var total = 0
        val left = (viewport.left + viewport.width * .075).toInt()
        val right = (viewport.left + viewport.width * .215).toInt()
        val top = (viewport.top + viewport.height * .635).toInt()
        val bottom = (viewport.top + viewport.height * .735).toInt()
        val step = (viewport.width / 240).coerceAtLeast(2)
        for (y in top until bottom step step) for (x in left until right step step) {
            val hsv = frame.rgbAt(x.coerceIn(0, frame.width - 1), y.coerceIn(0, frame.height - 1)).hsv()
            if (hsv.hue in 28..62 && hsv.saturation >= 90 && hsv.value >= 135) green++
            if (hsv.hue in 82..105 && hsv.saturation >= 75 && hsv.value >= 125) cyan++
            total++
        }
        val greenRatio = green.toDouble() / total.coerceAtLeast(1)
        val cyanRatio = cyan.toDouble() / total.coerceAtLeast(1)
        val available = greenRatio >= .012 && cyanRatio >= .055
        return HomeIdleRewardReading(available, NormalizedPoint(.135, .69).takeIf { available }, greenRatio, cyanRatio)
    }
}

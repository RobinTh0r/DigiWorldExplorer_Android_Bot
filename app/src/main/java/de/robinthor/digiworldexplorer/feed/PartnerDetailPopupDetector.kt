package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.*
import kotlin.math.abs

/** The large Partner detail card accidentally opened by a misplaced food-bubble tap. */
internal object PartnerDetailPopupDetector {
    fun detect(frame: PixelFrame): Boolean {
        val viewport = GameViewport.detect(frame)
        val step = (viewport.width / 540).coerceAtLeast(1)
        val cyan = ColorRegionLocator.find(frame, viewport, NormalizedRect(.04, .12, .96, .86), step) {
            val hsv = it.hsv(); hsv.hue in 85..110 && hsv.saturation > 95 && hsv.value > 130
        }
        val modal = cyan.filter {
            it.top in .16.. .32 && it.width in .65.. .93 && it.height in .42.. .68
        }.maxByOrNull { it.width } ?: return false
        val purple = ColorRegionLocator.find(frame, viewport, NormalizedRect(.08, .57, .92, .86), step) {
            val hsv = it.hsv(); hsv.hue in 125..165 && hsv.saturation > 95 && hsv.value > 100
        }.filter { it.width in .18.. .36 && it.height in .025.. .075 }
        val confirm = cyan.filter {
            it.top > modal.top + modal.height * .72 && it.bottom < modal.bottom &&
                it.width in .18.. .36 && it.height in .025.. .075
        }.singleOrNull() ?: return false
        val cancel = purple.singleOrNull { left ->
            left.right < confirm.left && confirm.left - left.right in .025.. .14 &&
                abs(left.center.y - confirm.center.y) < .015 &&
                abs(left.width - confirm.width) < .07
        } ?: return false
        // Both buttons must sit within the same modal, below its own wide header.
        return cancel.left >= modal.left && confirm.right <= modal.right &&
            frame.ratioInViewportPatch(viewport,
                NormalizedPoint(modal.center.x, modal.top + modal.height * .45),
                modal.width * .24, .06, step) {
                val hsv = it.hsv(); hsv.hue in 80..120 && hsv.saturation > 80 && hsv.value in 35..160
            } > .45
    }
}

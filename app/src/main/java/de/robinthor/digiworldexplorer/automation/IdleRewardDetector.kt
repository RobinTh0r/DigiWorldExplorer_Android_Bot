package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.*

data class IdleRewardReading(
    val screen: IdleRewardScreen,
    val claimTarget: NormalizedPoint? = null,
    val adTarget: NormalizedPoint? = null,
    val closeTarget: NormalizedPoint? = null,
)

enum class IdleRewardScreen { NONE, CLAIM, EMPTY, RESULT }

/** Reads the dialog outline and buttons from the captured image; text and capture size may vary. */
object IdleRewardDetector {
    fun detect(frame: PixelFrame): IdleRewardScreen = read(frame).screen

    fun read(frame: PixelFrame): IdleRewardReading {
        val viewport = GameViewport.fit(frame.width, frame.height)
        // Sample at roughly 360 columns so session-wide ownership stays cheap on high-DPI phones.
        val step = (viewport.width / 360).coerceAtLeast(1)
        val cyan = ColorRegionLocator.find(frame, viewport, NormalizedRect(.04, .14, .96, .86), step) {
            val hsv = it.hsv(); hsv.hue in 85..110 && hsv.saturation > 95 && hsv.value > 130
        }
        val resultOverlay = cyan.singleOrNull {
            it.width > .88 && it.height in .22.. .42 && it.top in .30.. .49 && it.bottom in .60.. .78
        }
        val idlePanelVisible = resultOverlay?.let { overlay ->
            val sideY = overlay.bottom + overlay.height * .25
            listOf(overlay.left + overlay.width * .16, overlay.right - overlay.width * .16).all { x ->
                frame.ratioInViewportPatch(viewport, NormalizedPoint(x, sideY), .006, .008, 1) {
                    it.blue > 55 && it.blue > it.red * 2 && it.green > it.red * 1.5
                } > .35
            }
        } == true
        if (resultOverlay != null && idlePanelVisible && cyan.any {
                it.top in .16.. .30 && it.width in .18.. .40 && it.bottom < resultOverlay.top
            }) {
            val closeY = (resultOverlay.bottom + resultOverlay.height * .48).coerceAtMost(.90)
            return IdleRewardReading(IdleRewardScreen.RESULT,
                closeTarget = NormalizedPoint(resultOverlay.center.x, closeY))
        }
        val modal = cyan.singleOrNull {
            it.width in .52.. .78 && it.height in .42.. .70 &&
                it.top in .15.. .38 && it.bottom in .68.. .86
        } ?: return IdleRewardReading(IdleRewardScreen.NONE)
        val claimButton = cyan.singleOrNull {
            it.left > modal.center.x && it.right < modal.right &&
                it.top > modal.top + modal.height * .78 && it.bottom < modal.bottom &&
                it.width in .17.. .33 && it.height in .035.. .075
        } ?: run {
            // On low-resolution captures the cyan button edge touches the modal border and
            // becomes one component. Recheck both button locations inside the measured modal.
            val center = NormalizedPoint(modal.left + modal.width * .706,
                modal.top + modal.height * .917)
            val edgeY = center.y + modal.height * .035
            val cyanEdge = frame.ratioInViewportPatch(viewport, NormalizedPoint(center.x, edgeY),
                modal.width * .10, modal.height * .007, 1) {
                val hsv = it.hsv(); hsv.hue in 85..110 && hsv.saturation > 95 && hsv.value > 120
            }
            if (cyanEdge < .45) return IdleRewardReading(IdleRewardScreen.NONE)
            NormalizedRect(center.x - modal.width * .18, center.y - modal.height * .05,
                center.x + modal.width * .18, center.y + modal.height * .05)
        }
        // The same button outline remains when the claim is disabled; its center turns dark.
        val enabled = frame.ratioInViewportPatch(viewport, claimButton.center,
            claimButton.width * .3, claimButton.height * .3, 1) {
            val hsv = it.hsv(); hsv.hue in 85..110 && hsv.saturation > 115 && hsv.value > 165
        } > .45
        val purple = ColorRegionLocator.find(frame, viewport,
            NormalizedRect(modal.left, claimButton.top - .03, modal.center.x, claimButton.bottom + .03), step) {
            val hsv = it.hsv(); hsv.hue in 125..165 && hsv.saturation > 95 && hsv.value > 100
        }.singleOrNull {
            it.width in .17.. .33 && it.height in .035.. .075 &&
                kotlin.math.abs(it.center.y - claimButton.center.y) < .02
        }
        return IdleRewardReading(if (enabled) IdleRewardScreen.CLAIM else IdleRewardScreen.EMPTY,
            claimTarget = claimButton.center.takeIf { enabled }, adTarget = purple?.center)
    }
}

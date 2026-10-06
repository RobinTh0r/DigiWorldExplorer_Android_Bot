package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame
import de.robinthor.digiworldexplorer.vision.ratioInViewportPatch

data class GameEntryReading(
    val screen: EntryScreen,
    val adRemaining: Int? = null,
    val viewport: GameViewport? = null,
    val target: NormalizedPoint? = null,
    val idleReading: IdleRewardReading? = null,
)

/** Conservative, language-independent recognition for the game's title and idle-reward flow. */
object GameEntryDetector {
    fun detect(frame: PixelFrame, partnerReading: de.robinthor.digiworldexplorer.feed.PartnerGrid? = null): GameEntryReading {
        val idleReading = IdleRewardDetector.read(frame)
        val idle = idleReading.screen
        if (idle != IdleRewardScreen.NONE) {
            val remaining = if (idle == IdleRewardScreen.RESULT) null else idleAdRemaining(frame, idleReading)
            return GameEntryReading(when (idle) {
                IdleRewardScreen.CLAIM -> EntryScreen.IDLE_CLAIM
                IdleRewardScreen.EMPTY -> EntryScreen.IDLE_EMPTY
                IdleRewardScreen.RESULT -> EntryScreen.RESULT
                IdleRewardScreen.NONE -> EntryScreen.UNKNOWN
            }, remaining, idleReading = idleReading)
        }
        val noticeHeader = ratio(frame, .20, .195, .82, .25) {
            it.blue > 145 && it.green > 90 && it.blue > it.red * 1.18
        }
        val noticePanel = ratio(frame, .16, .25, .84, .75) {
            it.blue > it.red * 1.25 && it.blue > it.green * 1.05 && it.red < 80
        }
        val noticeCards = ratio(frame, .20, .30, .80, .67) {
            it.red > 150 && it.green > 165 && it.blue > 175
        }
        if (noticeHeader > .35 && noticePanel > .30 && noticeCards > .18)
            return GameEntryReading(EntryScreen.NOTICE)

        // Fixed Home controls outrank artwork: stages may also be pink/purple like the title.
        if (HomeScreenDetector.detect(frame.width, frame.height, frame::argbAt))
            return GameEntryReading(EntryScreen.HOME)

        // Shared game chrome and positively recognized Partner screens cannot be a title.
        val partner = partnerReading ?: de.robinthor.digiworldexplorer.feed.PartnerGridDetector.detect(frame)
        if (partner.page || partner.confirmation) return GameEntryReading(EntryScreen.UNKNOWN)
        val titleViewport = TitleScreenEvidence.viewport(frame) ?: return GameEntryReading(EntryScreen.UNKNOWN)
        val startTarget = TitleScreenEvidence.touchTarget(frame, titleViewport)
        if (startTarget != null)
            return GameEntryReading(EntryScreen.LOGIN_READY, viewport = titleViewport, target = startTarget)
        return GameEntryReading(EntryScreen.LOGIN_LOADING, viewport = titleViewport)
    }

    /** Red numerator means 0/2. A visibly enabled purple button means at least one remains. */
    private fun idleAdRemaining(frame: PixelFrame, idle: IdleRewardReading): Int? {
        val button = idle.adTarget ?: return null
        val viewport = GameViewport.fit(frame.width, frame.height)
        val redZero = frame.ratioInViewportPatch(viewport,
            NormalizedPoint(button.x + .025, button.y - .015), .025, .018) {
            it.red > 125 && it.red > it.green * 1.45 && it.red > it.blue * 1.35
        }
        if (redZero > .025) return 0
        val enabledPurple = frame.ratioInViewportPatch(viewport, button, .08, .025) {
            it.blue > 65 && it.red > 45 && it.blue > it.green * 1.18
        }
        return if (enabledPurple > .30) 2 else null
    }

    private inline fun ratio(frame: PixelFrame, x0: Double, y0: Double, x1: Double, y1: Double,
                             match: (de.robinthor.digiworldexplorer.vision.Rgb) -> Boolean): Double {
        var hits = 0
        var total = 0
        val step = (frame.width / 240).coerceAtLeast(1)
        for (y in (frame.height*y0).toInt() until (frame.height*y1).toInt() step step)
            for (x in (frame.width*x0).toInt() until (frame.width*x1).toInt() step step) {
                if (match(frame.rgbAt(x, y))) hits++
                total++
            }
        return hits.toDouble() / total.coerceAtLeast(1)
    }
}

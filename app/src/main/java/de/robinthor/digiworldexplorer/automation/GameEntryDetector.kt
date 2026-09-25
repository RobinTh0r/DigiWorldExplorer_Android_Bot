package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame
import de.robinthor.digiworldexplorer.vision.ratioInViewportPatch

data class GameEntryReading(val screen: EntryScreen, val adRemaining: Int? = null)

/** Conservative, language-independent recognition for the game's title and idle-reward flow. */
object GameEntryDetector {
    fun detect(frame: PixelFrame): GameEntryReading {
        val idle = IdleRewardDetector.detect(frame)
        if (idle != IdleRewardScreen.NONE) {
            val remaining = if (idle == IdleRewardScreen.RESULT) null else idleAdRemaining(frame)
            return GameEntryReading(when (idle) {
                IdleRewardScreen.CLAIM -> EntryScreen.IDLE_CLAIM
                IdleRewardScreen.EMPTY -> EntryScreen.IDLE_EMPTY
                IdleRewardScreen.RESULT -> EntryScreen.RESULT
                IdleRewardScreen.NONE -> EntryScreen.UNKNOWN
            }, remaining)
        }
        val bottomNavigation = ratio(frame, .05, .90, .95, .995) {
            it.blue > 75 && it.blue > it.red * 1.35 && it.blue > it.green * 1.03
        }
        val rightActionRail = ratio(frame, .77, .07, .94, .32) {
            it.blue > 80 && it.blue > it.red * 1.35 && it.blue > it.green * 1.03
        }
        val centerBlue = ratio(frame, .22, .30, .72, .72) {
            it.blue > 80 && it.blue > it.red * 1.35 && it.blue > it.green * 1.03
        }
        if (HomeScreenDetector.detect(frame.width, frame.height, frame::argbAt) ||
            (bottomNavigation > .18 && rightActionRail > .08 && centerBlue < .45))
            return GameEntryReading(EntryScreen.HOME)

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

        val viewport = GameViewport.fit(frame.width, frame.height)
        val purple = frame.ratioInViewportPatch(viewport, NormalizedPoint(.50, .46), .38, .38) {
            it.red > 65 && it.blue > 80 && it.blue > it.green * 1.18 && it.red > it.green * 1.05
        }
        // Use full-frame coordinates here: the loading progress bar sits lower (about 91%) and
        // must never be confused with the wide Touch-to-Start button at about 84%.
        val cyanStart = ratio(frame, .20, .815, .80, .87) {
            it.blue > 110 && it.green > 80 && it.blue > it.red * 1.22
        }
        val whiteLabel = ratio(frame, .34, .825, .66, .86) {
            it.red > 185 && it.green > 185 && it.blue > 185
        }
        return when {
            purple > .24 && cyanStart > .44 && whiteLabel > .025 -> GameEntryReading(EntryScreen.LOGIN_READY)
            purple > .24 -> GameEntryReading(EntryScreen.LOGIN_LOADING)
            else -> GameEntryReading(EntryScreen.UNKNOWN)
        }
    }

    /** Red numerator means 0/2. A visibly enabled purple button means at least one remains. */
    private fun idleAdRemaining(frame: PixelFrame): Int? {
        val viewport = GameViewport.fit(frame.width, frame.height)
        val redZero = frame.ratioInViewportPatch(viewport, NormalizedPoint(.375, .725), .025, .018) {
            it.red > 125 && it.red > it.green * 1.45 && it.red > it.blue * 1.35
        }
        if (redZero > .025) return 0
        val enabledPurple = frame.ratioInViewportPatch(viewport, NormalizedPoint(.37, .738), .105, .035) {
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

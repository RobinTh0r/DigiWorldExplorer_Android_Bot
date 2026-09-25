package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class KnownPageDetectorTest {
    @Test fun detectsDungeonListFromThreeSeparatedWideRows() {
        assertEquals(ObservedScreen.DUNGEON_LIST, KnownPageDetector.detect(fixture(listOf(
            Box(.10, .34, .89, .47), Box(.10, .50, .89, .63), Box(.10, .66, .89, .79),
        ))))
    }

    @Test fun detectsPartnerPageFromHeroAndLowerPanels() {
        assertEquals(ObservedScreen.PARTNER_PAGE, KnownPageDetector.detect(fixture(listOf(
            Box(.13, .13, .87, .45), Box(.13, .46, .87, .58), Box(.13, .59, .87, .71), Box(.13, .72, .87, .84),
        ))))
    }

    @Test fun rejectsSingleSharedCyanPanel() {
        assertEquals(ObservedScreen.UNKNOWN, KnownPageDetector.detect(fixture(listOf(Box(.1, .2, .9, .35)))))
    }

    private data class Box(val left: Double, val top: Double, val right: Double, val bottom: Double)
    private fun fixture(boxes: List<Box>, width: Int = 360, height: Int = 640) = PixelFrame(width, height) { x, y ->
        if (boxes.any { x.toDouble() / width in it.left..it.right && y.toDouble() / height in it.top..it.bottom })
            0xff00ccee.toInt() else 0xff151021.toInt()
    }
}

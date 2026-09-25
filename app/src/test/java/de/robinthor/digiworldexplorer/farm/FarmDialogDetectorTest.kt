package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test

class FarmDialogDetectorTest {
    private val pixels = IntArray(360 * 640) { 0xff202840.toInt() }
    private fun frame() = PixelFrame(360, 640) { x, y -> pixels[y * 360 + x] }
    private fun rect(x: Int, y: Int, w: Int, h: Int, rgb: Int) {
        for (py in y until y + h) for (px in x until x + w) pixels[py * 360 + px] = rgb
    }

    @Test fun `three seed slots plus green select identify dialog without implying free cost`() {
        for (x in listOf(65, 160, 255)) rect(x, 290, 40, 35, 0xff964132.toInt())
        rect(130, 400, 100, 30, 0xff64dc14.toInt())
        val detected = FarmDialogDetector.detect(frame(), 3)
        assertEquals(FarmView.SEEDS, detected.view)
        assertEquals(3, detected.slots.size)
        assertNull(detected.selectedSlot)
        assertNotNull(detected.selectButton)
    }

    @Test fun `wide green button with field context is water but never a tap target`() {
        rect(80, 400, 200, 30, 0xff64dc14.toInt())
        val detected = FarmDialogDetector.detect(frame(), 3)
        assertEquals(FarmView.WATER, detected.view)
        assertNotNull(detected.selectButton)
        assertNotNull(detected.closeTarget)
        assertEquals(FarmView.UNKNOWN, FarmDialogDetector.detect(frame(), 0).view)
    }
}

package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlotTimerReaderTest {
    @Test fun `reads complete mm ss row and rejects missing colon`() {
        val pixels = IntArray(360 * 640) { 0xff202840.toInt() }
        fun rect(x: Int, y: Int, w: Int, h: Int) {
            for (py in y until y + h) for (px in x until x + w) pixels[py * 360 + px] = -1
        }
        fun one(x: Int) { rect(x + 3, 330, 2, 21); rect(x, 351, 8, 3) }
        // Plot 0 timer ROI: x~59..167, y~311..388. Four '1' glyphs and one colon.
        one(72); one(86); one(110); one(124)
        rect(101, 335, 3, 3); rect(101, 346, 3, 3)
        val frame = PixelFrame(360, 640) { x, y -> pixels[y * 360 + x] }
        assertEquals(11 * 60 + 11, PlotTimerReader.read(frame, 0))
        for (y in 335..337) for (x in 101..103) pixels[y * 360 + x] = 0xff202840.toInt()
        assertNull(PlotTimerReader.read(frame, 0))
    }
}

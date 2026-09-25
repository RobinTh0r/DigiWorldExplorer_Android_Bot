package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreeSeedCounterReaderTest {
    @Test fun `reads two separated compact glyphs and rejects three`() {
        val pixels = IntArray(360 * 640) { 0xff202840.toInt() }
        fun one(x: Int) {
            for (y in 42..62) for (px in x + 3..x + 4) pixels[y * 360 + px] = -1
            for (y in 63..65) for (px in x..x + 7) pixels[y * 360 + px] = -1
        }
        one(138); one(151)
        val frame = PixelFrame(360, 640) { x, y -> pixels[y * 360 + x] }
        assertEquals(11, FreeSeedCounterReader.read(frame))
        // Keep the third compact glyph fully inside the documented .375..455 counter ROI.
        for (y in 42..62) for (px in 160..161) pixels[y * 360 + px] = -1
        for (y in 63..65) for (px in 160..162) pixels[y * 360 + px] = -1
        assertNull(FreeSeedCounterReader.read(frame))
    }

    @Test fun `empty and tiny noise stay unknown`() {
        val pixels = IntArray(360 * 640) { 0xff202840.toInt() }
        val frame = PixelFrame(360, 640) { x, y -> pixels[y * 360 + x] }
        assertNull(FreeSeedCounterReader.read(frame))
        pixels[45 * 360 + 140] = -1
        assertNull(FreeSeedCounterReader.read(frame))
    }
}

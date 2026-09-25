package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test

class FarmHarvestDetectorTest {
    private val pixels = IntArray(360 * 640) { 0xff202840.toInt() }
    private fun frame() = PixelFrame(360, 640) { x, y -> pixels[y * 360 + x] }
    private fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        for (py in y until y + h) for (px in x until x + w) pixels[py * 360 + px] = color
    }
    private fun field() {
        FarmHarvestDetector.centers.forEach { p -> rect((p.x * 360).toInt() - 35, (p.y * 640).toInt() - 20, 70, 40, 0xffa84828.toInt()) }
    }

    @Test fun `separate soil plots plus badge number identify ripe plot`() {
        field()
        rect(87, 270, 50, 25, 0xff648214.toInt())
        rect(100, 276, 8, 15, 0xffffffff.toInt())
        rect(112, 276, 8, 15, 0xffffffff.toInt())
        val detection = FarmHarvestDetector.detect(frame())
        assertTrue(detection.field)
        assertEquals(PlotState.RIPE, detection.states[0])
        assertEquals(PlotState.UNKNOWN, detection.states[1])
    }

    @Test fun `uniform orange screen and isolated number are not a farm`() {
        pixels.fill(0xffa84828.toInt())
        assertFalse(FarmHarvestDetector.detect(frame()).field)
        pixels.fill(0xff202840.toInt())
        rect(87, 270, 50, 25, 0xff648214.toInt())
        rect(100, 276, 8, 15, 0xffffffff.toInt())
        rect(112, 276, 8, 15, 0xffffffff.toInt())
        assertFalse(FarmHarvestDetector.detect(frame()).field)
    }

    @Test fun `badge without bright digits is empty and single bright mark stays unknown`() {
        field()
        rect(87, 270, 50, 25, 0xff648214.toInt())
        assertEquals(PlotState.EMPTY, FarmHarvestDetector.detect(frame()).states[0])
        rect(100, 276, 8, 15, 0xffffffff.toInt())
        assertEquals(PlotState.UNKNOWN, FarmHarvestDetector.detect(frame()).states[0])
    }

    @Test fun `cyan bubble over inward tap blocks ripe plot`() {
        field()
        rect(87, 270, 50, 25, 0xff648214.toInt())
        rect(100, 276, 8, 15, 0xffffffff.toInt())
        rect(112, 276, 8, 15, 0xffffffff.toInt())
        // First plot's inward target is roughly x=.383, y=.507.
        // Hue 99 in OpenCV's 0..179 scale, inside the detector's cyan range.
        rect(131, 316, 15, 15, 0xff20b8f0.toInt())
        val detection = FarmHarvestDetector.detect(frame())
        assertEquals(PlotState.RIPE, detection.states[0])
        assertTrue(0 in detection.bubbleBlocked)
    }

    @Test fun `yellow lock marks only its plot as locked`() {
        field()
        FarmHarvestDetector.centers.forEachIndexed { index, p ->
            if (index == 1) return@forEachIndexed
            rect((p.x * 360).toInt() - 25, (p.y * 640).toInt() - 35, 50, 20, 0xff648214.toInt())
        }
        val locked = FarmHarvestDetector.centers[1]
        rect((locked.x * 360).toInt() - 7, (locked.y * 640).toInt() - 10, 14, 12, 0xffffdc20.toInt())
        val detection = FarmHarvestDetector.detect(frame())
        assertEquals(PlotState.LOCKED, detection.states[1])
        assertNotEquals(PlotState.LOCKED, detection.states[0])
    }

    @Test fun `two visually joined plots still form a field when all anchors are present`() {
        field()
        val a = FarmHarvestDetector.centers[1]
        val b = FarmHarvestDetector.centers[3]
        rect((a.x * 360).toInt() - 8, (a.y * 640).toInt(), 16, ((b.y - a.y) * 640).toInt(), 0xffa84828.toInt())
        assertTrue(FarmHarvestDetector.detect(frame()).field)
    }

    @Test fun `green progress bar safely marks a plot as growing without timer OCR`() {
        field()
        val plot = FarmHarvestDetector.centers[0]
        rect((plot.x * 360).toInt() - 20, (plot.y * 640).toInt() + 15, 40, 6, 0xff28d83c.toInt())
        assertEquals(PlotState.GROWING, FarmHarvestDetector.detect(frame()).states[0])
    }

    @Test fun `large pale harvest bubble marks the pointed plot ripe`() {
        field()
        val plot = FarmHarvestDetector.centers[0]
        rect((plot.x * 360).toInt() - 28, (plot.y * 640).toInt() - 60, 58, 40, 0xfffff5d8.toInt())
        rect((plot.x * 360).toInt() - 15, (plot.y * 640).toInt() - 52, 30, 28, 0xff8d3828.toInt())
        assertEquals(PlotState.RIPE, FarmHarvestDetector.detect(frame()).states[0])
    }

    @Test fun `large pale shovel bubble marks the pointed plot empty`() {
        field()
        val plot = FarmHarvestDetector.centers[0]
        rect((plot.x * 360).toInt() - 28, (plot.y * 640).toInt() - 60, 58, 40, 0xfffff5d8.toInt())
        rect((plot.x * 360).toInt() - 3, (plot.y * 640).toInt() - 49, 6, 20, 0xff949aa5.toInt())
        assertEquals(PlotState.EMPTY, FarmHarvestDetector.detect(frame()).states[0])
    }
}

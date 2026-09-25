package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedPoint
import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeatFieldScreenDetectorTest {
    @Test fun `recognizes all six plot regions`() {
        val image = SyntheticImage(360, 640, 0xff202840.toInt())
        val viewport = GameViewport.fit(image.width, image.height)
        listOf(.507, .653, .810).forEach { y ->
            listOf(.313, .640).forEach { x -> image.fillPatch(viewport, NormalizedPoint(x, y), 15, 10, 0xffa84828.toInt()) }
        }
        assertTrue(MeatFieldScreenDetector.detect(image.frame(), viewport).recognized)
    }

    @Test fun `rejects unrelated red area without complete lattice`() {
        val image = SyntheticImage(360, 640, 0xff202840.toInt())
        val viewport = GameViewport.fit(image.width, image.height)
        image.fillPatch(viewport, NormalizedPoint(.313, .507), 15, 10, 0xffa84828.toInt())
        assertFalse(MeatFieldScreenDetector.detect(image.frame(), viewport).recognized)
    }

    @Test fun `rejects lattice with one missing plot`() {
        val image = SyntheticImage(360, 640, 0xff202840.toInt())
        val viewport = GameViewport.fit(image.width, image.height)
        val centers = listOf(.507, .653, .810).flatMap { y -> listOf(.313, .640).map { x -> NormalizedPoint(x, y) } }
        centers.dropLast(1).forEach { image.fillPatch(viewport, it, 15, 10, 0xffa84828.toInt()) }
        assertFalse(MeatFieldScreenDetector.detect(image.frame(), viewport).recognized)
    }

    private class SyntheticImage(val width: Int, val height: Int, initial: Int) {
        private val pixels = IntArray(width * height) { initial }
        fun frame() = PixelFrame(width, height) { x, y -> pixels[y * width + x] }
        fun fillPatch(viewport: GameViewport, center: NormalizedPoint, rx: Int, ry: Int, color: Int) {
            val (cx, cy) = viewport.pixel(center)
            for (y in cy - ry..cy + ry) for (x in cx - rx..cx + rx) {
                if (x in 0 until width && y in 0 until height) pixels[y * width + x] = color
            }
        }
    }
}

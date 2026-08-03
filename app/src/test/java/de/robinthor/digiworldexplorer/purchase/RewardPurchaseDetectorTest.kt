package de.robinthor.digiworldexplorer.purchase

import org.junit.Assert.*
import org.junit.Test

class RewardPurchaseDetectorTest {
    private val width = 270
    private val height = 600

    private fun screen(redCost: Boolean = false, modal: Boolean = false): IntArray {
        val pixels = IntArray(width * height) { rgb(20, 80, 140) }
        if (!modal) {
            fill(pixels, .50, .925, .82, .995, rgb(240, 190, 20))
            fill(pixels, .15, .925, .49, .995, rgb(20, 130, 220))
            fill(pixels, .83, .925, .98, .995, rgb(235, 240, 245))
            if (redCost) fill(pixels, .62, .90, .65, .93, rgb(210, 35, 35))
        }
        return pixels
    }

    @Test fun recognizesAffordableRewardScreen() {
        val pixels = screen()
        val result = RewardPurchaseDetector.detect(width, height) { x, y -> pixels[y * width + x] }
        assertTrue(result.recognized)
        assertTrue(result.affordable)
        assertEquals(width * .665f, result.tapX, .1f)
    }

    @Test fun redThirtyStopsPurchasing() {
        val pixels = screen(redCost = true)
        val result = RewardPurchaseDetector.detect(width, height) { x, y -> pixels[y * width + x] }
        assertTrue(result.recognized)
        assertFalse(result.affordable)
    }

    @Test fun shopModalIsNotRecognized() {
        val pixels = screen(modal = true)
        val result = RewardPurchaseDetector.detect(width, height) { x, y -> pixels[y * width + x] }
        assertFalse(result.recognized)
        assertFalse(result.affordable)
    }

    private fun fill(pixels: IntArray, x0: Double, y0: Double, x1: Double, y1: Double, color: Int) {
        for (y in (height * y0).toInt() until (height * y1).toInt())
            for (x in (width * x0).toInt() until (width * x1).toInt()) pixels[y * width + x] = color
    }

    private fun rgb(r: Int, g: Int, b: Int) = (255 shl 24) or (r shl 16) or (g shl 8) or b
}

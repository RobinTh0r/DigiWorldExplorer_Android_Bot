package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenDetectorTest {
    private val width = 360
    private val height = 640

    @Test fun requiresAllThreeIndependentRegions() {
        assertTrue(HomeScreenDetector.detect(width, height, frame()))
        assertFalse(HomeScreenDetector.detect(width, height, frame(includeNavigation = false)))
        assertFalse(HomeScreenDetector.detect(width, height) { _, _ -> 0xff102a52.toInt() })
    }

    private fun frame(includeNavigation: Boolean = true): (Int, Int) -> Int = { x, y ->
        val fx = x.toDouble() / width
        val fy = y.toDouble() / height
        when {
            includeNavigation && fx in .40.. .60 && fy in .91.. .995 -> 0xff168fd8.toInt()
            fx in .05.. .95 && fy in .72.. .92 -> 0xff102a52.toInt()
            fx in .18.. .78 && fy in .14.. .68 -> 0xffe8dfbd.toInt()
            else -> 0xff26384a.toInt()
        }
    }
}

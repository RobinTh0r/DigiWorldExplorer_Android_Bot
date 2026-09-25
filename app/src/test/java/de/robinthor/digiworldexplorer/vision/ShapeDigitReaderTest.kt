package de.robinthor.digiworldexplorer.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeDigitReaderTest {
    @Test fun `two enclosed regions classify eight`() {
        val bits = BooleanArray(12 * 16)
        for (r in 0 until 16) for (c in 1..10) {
            val ring = c == 1 || c == 10 || r in setOf(0, 7, 8, 15)
            if (ring) bits[r * 12 + c] = true
        }
        assertEquals(8, ShapeDigitReader.classify(bits, 12, ColorComponent(0, 0, 11, 15, bits.count { it })))
    }

    @Test fun `narrow stem with base classifies one`() {
        val bits = BooleanArray(12 * 16)
        for (r in 0 until 14) for (c in 5..6) bits[r * 12 + c] = true
        for (r in 14..15) for (c in 0 until 12) bits[r * 12 + c] = true
        assertEquals(1, ShapeDigitReader.classify(bits, 12, ColorComponent(0, 0, 11, 15, bits.count { it })))
    }
}

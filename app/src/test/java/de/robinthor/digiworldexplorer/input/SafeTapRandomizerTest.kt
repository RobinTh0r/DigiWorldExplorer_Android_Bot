package de.robinthor.digiworldexplorer.input

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeTapRandomizerTest {
    @Test fun pointsNeverLeaveSafeRectangle() {
        repeat(10_000) {
            val (x,y)=SafeTapRandomizer.point(100f,200f,8f,12f,Random(it))
            assertTrue(x in 92f..108f);assertTrue(y in 188f..212f)
        }
    }
    @Test fun delaysStayInsideConfiguredRange() {
        repeat(10_000) { assertTrue(SafeTapRandomizer.delay(800,60,Random(it)) in 740L..860L) }
    }
}
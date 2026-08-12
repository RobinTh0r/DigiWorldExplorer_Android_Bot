package de.robinthor.digiworldexplorer.input

import kotlin.random.Random

/** Kleine Eingabevarianz, die immer innerhalb eines vom Aufrufer garantierten Innenradius bleibt. */
object SafeTapRandomizer {
    fun point(x: Float, y: Float, safeRadiusX: Float, safeRadiusY: Float, random: Random = Random.Default): Pair<Float, Float> {
        val rx = safeRadiusX.coerceAtLeast(0f)
        val ry = safeRadiusY.coerceAtLeast(0f)
        return x + random.nextFloat(-rx, rx) to y + random.nextFloat(-ry, ry)
    }

    fun delay(baseMs: Long, variationMs: Long, random: Random = Random.Default): Long {
        val spread = variationMs.coerceIn(0L, baseMs.coerceAtLeast(0L))
        return if (spread == 0L) baseMs else random.nextLong(baseMs - spread, baseMs + spread + 1L)
    }

    private fun Random.nextFloat(from: Float, until: Float): Float = from + nextFloat() * (until - from)
}
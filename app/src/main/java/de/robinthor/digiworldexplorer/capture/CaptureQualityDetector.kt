package de.robinthor.digiworldexplorer.capture

import kotlin.math.sqrt

enum class CaptureQuality { VALID, UNIFORM_GRAY, BLACK }

data class CaptureQualityResult(
    val quality: CaptureQuality,
    val meanLuma: Double,
    val lumaDeviation: Double,
    val meanChroma: Double,
)

/** Detects a missing compositor surface without relying on game language or resolution. */
object CaptureQualityDetector {
    private const val COLS = 24
    private const val ROWS = 24

    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): CaptureQualityResult {
        var lumaSum = 0.0
        var lumaSquareSum = 0.0
        var chromaSum = 0.0
        var count = 0
        // Ignore HUD/status areas. The Unity game surface occupies the large center on phones/emulators.
        for (row in 0 until ROWS) for (col in 0 until COLS) {
            val x = (width * (.10 + .80 * (col + .5) / COLS)).toInt().coerceIn(0, width - 1)
            val y = (height * (.20 + .60 * (row + .5) / ROWS)).toInt().coerceIn(0, height - 1)
            val pixel = argbAt(x, y)
            val r = pixel shr 16 and 255
            val g = pixel shr 8 and 255
            val b = pixel and 255
            val luma = (r * 3 + g * 6 + b) / 10.0
            lumaSum += luma
            lumaSquareSum += luma * luma
            chromaSum += maxOf(r, g, b) - minOf(r, g, b)
            count++
        }
        val mean = lumaSum / count
        val deviation = sqrt((lumaSquareSum / count - mean * mean).coerceAtLeast(0.0))
        val chroma = chromaSum / count
        val quality = when {
            mean < 18.0 && deviation < 12.0 -> CaptureQuality.BLACK
            deviation < 9.0 && chroma < 12.0 -> CaptureQuality.UNIFORM_GRAY
            else -> CaptureQuality.VALID
        }
        return CaptureQualityResult(quality, mean, deviation, chroma)
    }
}
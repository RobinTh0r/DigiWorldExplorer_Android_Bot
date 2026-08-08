package de.robinthor.digiworldexplorer.network

enum class NetworkDefenseScreen { NONE, START, DIABOROMON }

data class NetworkDefenseDetection(
    val screen: NetworkDefenseScreen,
    val tapXRatio: Float,
    val tapYRatio: Float,
    val confidence: Double,
)

/** Language-independent recognition based on the unique Network Defense Ops layouts. */
object NetworkDefenseScreenDetector {
    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): NetworkDefenseDetection {
        val titleCyan = ratio(width, height, .15, .14, .85, .24, argbAt, ::cyan)
        val attemptCyan = ratio(width, height, .48, .53, .82, .61, argbAt, ::cyan)
        val previousPurple = ratio(width, height, .17, .53, .50, .61, argbAt, ::purple)
        val partyCyan = ratio(width, height, .27, .73, .68, .82, argbAt, ::cyan)
        val startPanelNavy = ratio(width, height, .08, .18, .92, .76, argbAt, ::navy)
        val startScore = titleCyan + attemptCyan + previousPurple + partyCyan + startPanelNavy

        // The banner spans the complete screen. Requiring both edges prevents red battle sprites or
        // effects in the middle from triggering the give-up action before Diaboromon appears.
        val bannerLeft = ratio(width, height, .00, .43, .20, .58, argbAt, ::darkOrRed)
        val bannerCenter = ratio(width, height, .20, .43, .80, .58, argbAt, ::darkOrRed)
        val bannerRight = ratio(width, height, .80, .43, 1.0, .58, argbAt, ::darkOrRed)
        val giveUpPanelNavy = ratio(width, height, .00, .75, 1.0, 1.0, argbAt, ::navy)
        val giveUpButtonCyan = ratio(width, height, .33, .88, .67, .96, argbAt, ::cyan)
        val waveFiveDistance = waveFiveDistance(width, height, argbAt)
        val bossScore = bannerLeft + bannerCenter + bannerRight + giveUpPanelNavy + giveUpButtonCyan + (1.0 - waveFiveDistance)

        return when {
            bannerLeft >= .32 && bannerCenter >= .30 && bannerRight >= .25 &&
                giveUpPanelNavy >= .75 && giveUpButtonCyan >= .18 && waveFiveDistance <= .14 ->
                NetworkDefenseDetection(NetworkDefenseScreen.DIABOROMON, .50f, .925f, bossScore)
            titleCyan >= .28 && attemptCyan >= .25 && previousPurple >= .18 && partyCyan >= .14 && startPanelNavy >= .48 ->
                NetworkDefenseDetection(NetworkDefenseScreen.START, .65f, .57f, startScore)
            else -> NetworkDefenseDetection(NetworkDefenseScreen.NONE, 0f, 0f, maxOf(startScore, bossScore))
        }
    }

    /** Normalized 5/5 mask sampled from the wave counter, independent of capture resolution. */
    private fun waveFiveDistance(width: Int, height: Int, argbAt: (Int, Int) -> Int): Double {
        val template = arrayOf(
            "0000000000000000", "0000000000000000", "0000000000000000", "0111011111000000",
            "0101101011000000", "0110111101000000", "0111111111000000", "0001100000000000",
            "0000001000000000", "0000110000000000",
        )
        val x0 = width * .365
        val y0 = height * .142
        val regionWidth = width * (.475 - .365)
        val regionHeight = height * (.185 - .142)
        var different = 0
        var total = 0
        for (row in template.indices) for (col in template[row].indices) {
            val sx0 = (x0 + regionWidth * col / 16.0).toInt().coerceIn(0, width - 1)
            val sx1 = (x0 + regionWidth * (col + 1) / 16.0).toInt().coerceIn(sx0 + 1, width)
            val sy0 = (y0 + regionHeight * row / 10.0).toInt().coerceIn(0, height - 1)
            val sy1 = (y0 + regionHeight * (row + 1) / 10.0).toInt().coerceIn(sy0 + 1, height)
            var luma = 0L
            var samples = 0
            val stepX = ((sx1 - sx0) / 3).coerceAtLeast(1)
            val stepY = ((sy1 - sy0) / 3).coerceAtLeast(1)
            for (y in sy0 until sy1 step stepY) for (x in sx0 until sx1 step stepX) {
                val p = argbAt(x, y)
                luma += ((p shr 16 and 255) * 3 + (p shr 8 and 255) * 6 + (p and 255)) / 10
                samples++
            }
            val dark = luma / samples.coerceAtLeast(1) < 160
            if (dark != (template[row][col] == '1')) different++
            total++
        }
        return different / total.toDouble()
    }

    private fun cyan(r: Int, g: Int, b: Int) = b > 130 && g > 80 && b > r * 1.25 && g > r * 1.05
    private fun purple(r: Int, g: Int, b: Int) = r > 80 && b > 100 && b > g * 1.15
    private fun navy(r: Int, g: Int, b: Int) = b > 35 && b > r * 1.15 && b > g * .72 && r < 80 && g < 130
    private fun darkOrRed(r: Int, g: Int, b: Int) = (r < 85 && g < 75 && b < 85) || (r > 90 && r > g * 1.30 && r > b * 1.05)

    private inline fun ratio(
        width: Int, height: Int, x0: Double, y0: Double, x1: Double, y1: Double,
        argbAt: (Int, Int) -> Int,
        match: (Int, Int, Int) -> Boolean,
    ): Double {
        val step = (width / 240).coerceAtLeast(2)
        var hits = 0
        var total = 0
        for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
            for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                val pixel = argbAt(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
                if (match(pixel shr 16 and 255, pixel shr 8 and 255, pixel and 255)) hits++
                total++
            }
        }
        return hits / total.coerceAtLeast(1).toDouble()
    }
}
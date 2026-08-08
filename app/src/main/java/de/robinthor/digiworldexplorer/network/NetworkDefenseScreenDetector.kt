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
        val startTicketPurple = ratio(width, height, .70, .09, .88, .17, argbAt, ::purple)
        val startScore = titleCyan + attemptCyan + previousPurple + partyCyan + startPanelNavy + startTicketPurple

        // The banner spans the complete screen. Requiring both edges prevents red battle sprites or
        // effects in the middle from triggering the give-up action before Diaboromon appears.
        val waveDemonRed = ratio(width, height, .25, .13, .38, .20, argbAt, ::brightRed)
        val bannerLeft = ratio(width, height, .00, .43, .20, .58, argbAt, ::darkOrRed)
        val bannerCenter = ratio(width, height, .20, .43, .80, .58, argbAt, ::darkOrRed)
        val bannerRight = ratio(width, height, .80, .43, 1.0, .58, argbAt, ::darkOrRed)
        val giveUpPanelNavy = ratio(width, height, .00, .75, 1.0, 1.0, argbAt, ::navy)
        val giveUpButtonCyan = ratio(width, height, .25, .86, .75, .99, argbAt, ::cyan)
        val giveUpButtonCenter = centerOfMatch(width, height, .25, .86, .75, .99, argbAt, ::cyan)
        val bossScore = waveDemonRed + bannerLeft + bannerCenter + bannerRight + giveUpPanelNavy + giveUpButtonCyan

        return when {
            // Full-width banner + give-up panel are stable across scaling and languages.
            waveDemonRed >= .015 && bannerLeft >= .20 && bannerCenter >= .24 && bannerRight >= .18 &&
                giveUpPanelNavy >= .55 && giveUpButtonCyan >= .10 ->
                NetworkDefenseDetection(
                    NetworkDefenseScreen.DIABOROMON,
                    giveUpButtonCenter?.first ?: .50f,
                    ((giveUpButtonCenter?.second ?: .925f) + .035f).coerceAtMost(.98f),
                    bossScore,
                )
            titleCyan >= .28 && attemptCyan >= .25 && previousPurple >= .18 && partyCyan >= .14 &&
                startPanelNavy >= .48 && startTicketPurple >= .01 ->
                NetworkDefenseDetection(NetworkDefenseScreen.START, .65f, .57f, startScore)
            else -> NetworkDefenseDetection(NetworkDefenseScreen.NONE, 0f, 0f, maxOf(startScore, bossScore))
        }
    }

    private fun cyan(r: Int, g: Int, b: Int) = b > 130 && g > 80 && b > r * 1.25 && g > r * 1.05
    private fun purple(r: Int, g: Int, b: Int) = r > 80 && b > 100 && b > g * 1.15
    private fun navy(r: Int, g: Int, b: Int) = b > 35 && b > r * 1.15 && b > g * .72 && r < 80 && g < 130
    private fun brightRed(r: Int, g: Int, b: Int) = r > 125 && r > g * 1.35 && r > b * 1.10
    private fun darkOrRed(r: Int, g: Int, b: Int) = (r < 85 && g < 75 && b < 85) || (r > 90 && r > g * 1.30 && r > b * 1.05)

    private inline fun centerOfMatch(
        width: Int, height: Int, x0: Double, y0: Double, x1: Double, y1: Double,
        argbAt: (Int, Int) -> Int,
        match: (Int, Int, Int) -> Boolean,
    ): Pair<Float, Float>? {
        val step = (width / 300).coerceAtLeast(2)
        var sumX = 0L
        var sumY = 0L
        var hits = 0
        for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
            for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                val pixel = argbAt(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
                if (match(pixel shr 16 and 255, pixel shr 8 and 255, pixel and 255)) {
                    sumX += x
                    sumY += y
                    hits++
                }
            }
        }
        if (hits < 8) return null
        return Pair((sumX.toDouble() / hits / width).toFloat(), (sumY.toDouble() / hits / height).toFloat())
    }
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
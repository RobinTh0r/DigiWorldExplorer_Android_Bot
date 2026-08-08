package de.robinthor.digiworldexplorer.network

enum class NetworkDefenseScreen { NONE, START, DIABOROMON }

data class NetworkDefenseDetection(
    val screen: NetworkDefenseScreen,
    val tapX: Float,
    val tapY: Float,
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

        val bannerRed = ratio(width, height, .00, .42, 1.0, .58, argbAt, ::red)
        val giveUpPanelNavy = ratio(width, height, .00, .75, 1.0, 1.0, argbAt, ::navy)
        val giveUpButtonCyan = ratio(width, height, .33, .88, .67, .96, argbAt, ::cyan)
        val battleLight = ratio(width, height, .00, .05, 1.0, .42, argbAt, ::light)
        val bossScore = bannerRed * 2.0 + giveUpPanelNavy + giveUpButtonCyan + battleLight

        return when {
            bannerRed >= .10 && giveUpPanelNavy >= .75 && giveUpButtonCyan >= .18 && battleLight >= .30 ->
                NetworkDefenseDetection(NetworkDefenseScreen.DIABOROMON, width * .50f, height * .925f, bossScore)
            titleCyan >= .35 && attemptCyan >= .32 && previousPurple >= .25 && partyCyan >= .20 && startPanelNavy >= .58 ->
                NetworkDefenseDetection(NetworkDefenseScreen.START, width * .65f, height * .57f, startScore)
            else -> NetworkDefenseDetection(NetworkDefenseScreen.NONE, 0f, 0f, maxOf(startScore, bossScore))
        }
    }

    private fun cyan(r: Int, g: Int, b: Int) = b > 130 && g > 80 && b > r * 1.25 && g > r * 1.05
    private fun purple(r: Int, g: Int, b: Int) = r > 80 && b > 100 && b > g * 1.15
    private fun navy(r: Int, g: Int, b: Int) = b > 35 && b > r * 1.15 && b > g * .72 && r < 80 && g < 130
    private fun red(r: Int, g: Int, b: Int) = r > 90 && r > g * 1.30 && r > b * 1.05
    private fun light(r: Int, g: Int, b: Int) = r > 190 && g > 190 && b > 190

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
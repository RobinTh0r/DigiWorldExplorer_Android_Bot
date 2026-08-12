package de.robinthor.digiworldexplorer.purchase

data class RewardPurchaseDetection(
    val recognized: Boolean,
    val affordable: Boolean,
    val tapX: Float,
    val tapY: Float,
    val yellowRatio: Double,
    val blueRatio: Double,
    val closeRatio: Double,
    val redCostRatio: Double,
)

object RewardPurchaseDetector {
    private const val YELLOW_MIN = .45
    private const val BLUE_MIN = .40
    private const val CLOSE_MIN = .25
    private const val RED_COST_MIN = .010

    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): RewardPurchaseDetection {
        val yellow = ratio(width, height, .50, .925, .82, .995, argbAt) { r, g, b -> r > 180 && g > 120 && b < 100 }
        val blue = ratio(width, height, .15, .925, .49, .995, argbAt) { r, g, b -> b > 130 && g > 70 && r < 100 }
        val close = ratio(width, height, .83, .925, .98, .995, argbAt) { r, g, b -> r > 180 && g > 180 && b > 180 }
        val redCost = ratio(width, height, .57, .88, .72, .94, argbAt) { r, g, b -> r > 150 && g < 110 && b < 110 }
        val recognized = yellow >= YELLOW_MIN && blue >= BLUE_MIN && close >= CLOSE_MIN
        return RewardPurchaseDetection(recognized, recognized && redCost < RED_COST_MIN, width * .665f, height * .96f, yellow, blue, close, redCost)
    }

    private inline fun ratio(
        width: Int, height: Int, x0: Double, y0: Double, x1: Double, y1: Double,
        argbAt: (Int, Int) -> Int,
        match: (Int, Int, Int) -> Boolean,
    ): Double {
        val step = (width / 270).coerceAtLeast(2)
        var hits = 0
        var total = 0
        for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
            for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                val p = argbAt(x, y)
                if (match(p shr 16 and 255, p shr 8 and 255, p and 255)) hits++
                total++
            }
        }
        return hits / total.coerceAtLeast(1).toDouble()
    }
}

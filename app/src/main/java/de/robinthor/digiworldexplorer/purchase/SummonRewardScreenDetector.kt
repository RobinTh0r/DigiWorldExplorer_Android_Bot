package de.robinthor.digiworldexplorer.purchase

/** The 5-column card reveal is not the summon menu, even though both show buy buttons. */
object SummonRewardScreenDetector {
    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): Boolean {
        if (width < 200 || height < 400) return false
        // Card wells have dark navy gutters at the same five-column positions on phone screens.
        // Require several rows so a single blue dialog cannot masquerade as the reward grid.
        var wells = 0
        for (y in listOf(.275, .36, .45, .54, .63, .72, .80)) {
            for (x in listOf(.165, .365, .575, .775, .96)) {
                val p = argbAt((width * x).toInt(), (height * y).toInt())
                val r = p shr 16 and 255
                val g = p shr 8 and 255
                val b = p and 255
                if (r < 55 && g in 35..125 && b in 75..175 && b > g) wells++
            }
        }
        if (wells < 13) return false
        // The result's full-width cyan band sits directly below the last card row.
        var cyan = 0
        for (x in listOf(.08, .20, .32, .44, .56, .68, .80, .92)) {
            val p = argbAt((width * x).toInt(), (height * .85).toInt())
            val r = p shr 16 and 255
            val g = p shr 8 and 255
            val b = p and 255
            if (r < 110 && g > 120 && b > 155 && b > g) cyan++
        }
        return cyan >= 6
    }
}

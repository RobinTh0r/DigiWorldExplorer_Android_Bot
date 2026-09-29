package de.robinthor.digiworldexplorer.purchase

/** The 5-column card reveal is not the summon menu, even though both show buy buttons. */
enum class SummonRewardState { NONE, REVEAL, RESULT }

object SummonRewardScreenDetector {
    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int) =
        detectState(width, height, argbAt) != SummonRewardState.NONE

    fun detectState(width: Int, height: Int, argbAt: (Int, Int) -> Int): SummonRewardState {
        if (width < 200 || height < 400) return SummonRewardState.NONE
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
        // Crest results can contain only two rows. Their ten compact wells are still a proper
        // five-column grid, and the verified summon button below separates them from generic
        // blue reward/dialog screens.
        var compactWells = 0
        for (y in listOf(.275, .375)) for (x in listOf(.10, .30, .50, .70, .90)) {
            val p = argbAt((width * x).toInt(), (height * y).toInt())
            val r = p shr 16 and 255; val g = p shr 8 and 255; val b = p and 255
            if (r < 70 && g in 30..135 && b in 65..190 && b > g) compactWells++
        }
        val summonButton = RewardPurchaseDetector.detect(width, height, argbAt).recognized
        if (wells < 13 && !(compactWells >= 6 && summonButton)) return SummonRewardState.NONE
        // The result's full-width cyan band sits directly below the last card row.
        var cyan = 0
        for (x in listOf(.08, .20, .32, .44, .56, .68, .80, .92)) {
            val p = argbAt((width * x).toInt(), (height * .85).toInt())
            val r = p shr 16 and 255
            val g = p shr 8 and 255
            val b = p and 255
            if (r < 110 && g > 120 && b > 155 && b > g) cyan++
        }
        if (cyan < 6) return SummonRewardState.NONE
        // A completed result has the pale "Reward" title. During the preceding reveal animation
        // the same card grid and buy buttons are already visible, so grid evidence alone cannot
        // authorize another purchase.
        var paleTitle = 0
        var titleSamples = 0
        val step = (width / 270).coerceAtLeast(2)
        for (y in (height * .17).toInt() until (height * .24).toInt() step step) {
            for (x in (width * .25).toInt() until (width * .75).toInt() step step) {
                val p = argbAt(x, y)
                val r = p shr 16 and 255; val g = p shr 8 and 255; val b = p and 255
                val high = maxOf(r, g, b); val low = minOf(r, g, b)
                if (high > 145 && high - low < 70) paleTitle++
                titleSamples++
            }
        }
        return if (paleTitle.toDouble() / titleSamples.coerceAtLeast(1) >= .004)
            SummonRewardState.RESULT else SummonRewardState.REVEAL
    }
}

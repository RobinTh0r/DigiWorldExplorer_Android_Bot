package de.robinthor.digiworldexplorer.dungeon

enum class DungeonScreen { NONE, CHALLENGE, REWARD }

data class DungeonDetection(
    val screen: DungeonScreen,
    val tapX: Float,
    val tapY: Float,
    val confidence: Double,
)

/** Language-independent detector using large colour/layout regions, never translated labels. */
object DungeonScreenDetector {
    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): DungeonDetection {
        val challengeButton = ratio(width, height, .49, .735, .84, .835, argbAt, ::cyan)
        val challengePanel = ratio(width, height, .08, .22, .92, .78, argbAt, ::navy)
        val challengeFrame = ratio(width, height, .07, .18, .93, .84, argbAt, ::cyan)
        val challengeScore = challengeButton * 2.2 + challengePanel * .7 + challengeFrame * .5
        val challengeDistance = challengeTemplateDistance(width, height, argbAt)

        val rewardBlue = ratio(width, height, .04, .18, .96, .74, argbAt, ::blueOverlay)
        val rewardTiles = ratio(width, height, .10, .29, .86, .47, argbAt, ::navy)
        val rewardDistance = rewardTemplateDistance(width, height, argbAt)
        val rewardScore = 1.0 - rewardDistance

        return when {
            challengeButton >= .10 && challengePanel >= .28 && challengeFrame >= .025 && challengeDistance <= CHALLENGE_TEMPLATE_MAX_DISTANCE ->
                DungeonDetection(DungeonScreen.CHALLENGE, width * .64f, height * .78f, challengeScore)
            rewardBlue >= .55 && rewardTiles >= .45 && rewardDistance <= REWARD_TEMPLATE_MAX_DISTANCE ->
                DungeonDetection(DungeonScreen.REWARD, width * .50f, height * .66f, rewardScore)
            else -> DungeonDetection(DungeonScreen.NONE, 0f, 0f, maxOf(challengeScore, rewardScore))
        }
    }

    private fun cyan(r: Int, g: Int, b: Int) = b > 145 && g > 95 && b > r * 1.35 && g > r * 1.15
    private fun navy(r: Int, g: Int, b: Int) = b > 45 && b > r * 1.20 && b > g * .75 && r < 70 && g < 115
    private fun blueOverlay(r: Int, g: Int, b: Int) = b > 95 && b > r * 1.20 && b > g * .85 && r < 125

    private const val CHALLENGE_TEMPLATE_MAX_DISTANCE = .12
    private val challengeTemplate = intArrayOf(
        0x132C2C, 0x0E2221, 0x0F262D, 0x0F2831, 0x152D29, 0x0E252D, 0x0F272E, 0x0E262D, 0x102927, 0x061925, 0x122522, 0x071A26,
        0x10374B, 0x34323E, 0x365F6D, 0x0D2C4E, 0x1F414D, 0x1B333E, 0x041A2D, 0x194D38, 0x465963, 0x1B5E66, 0x06243A, 0x0D3648,
        0x052C49, 0x114765, 0x164D68, 0x1E4D67, 0x275567, 0x305B67, 0x2E5765, 0x0B2746, 0x214D67, 0x0E3E62, 0x17507C, 0x052C49,
        0x000A1B, 0x2D4F5C, 0x115D77, 0x296579, 0x084055, 0x286478, 0x064054, 0x2A667A, 0x296479, 0x205D73, 0x171E38, 0x00091B,
        0x000A1A, 0x0C488B, 0x09396D, 0x083B6E, 0x093B6E, 0x093B6E, 0x165A97, 0x184073, 0x1D428A, 0x09316C, 0x0C488B, 0x000A1A,
        0x000B1D, 0x3B91C4, 0x605E71, 0x2E4864, 0x425E78, 0x4A627B, 0x5E4E30, 0x362F25, 0x5A485E, 0x23272E, 0x175A99, 0x000B1D,
        0x000819, 0x0C3A72, 0x0A244A, 0x0A244A, 0x14205A, 0x162558, 0x162558, 0x213367, 0x341F3D, 0x0A244A, 0x0C488B, 0x000819,
        0x000B1D, 0x0C3365, 0x051836, 0x051836, 0x3F4E68, 0x565C72, 0x8F94A2, 0x040F2B, 0x051836, 0x051836, 0x0C3970, 0x000B1C,
        0x000819, 0x0C3365, 0x051836, 0x051836, 0x000009, 0x051836, 0x27072C, 0x7C8A9E, 0x051836, 0x051836, 0x0C3970, 0x000819,
        0x000C1D, 0x0C3365, 0x051836, 0x051836, 0x051836, 0x051B3A, 0x051836, 0x051836, 0x051836, 0x051836, 0x0C3970, 0x000A1B,
        0x000919, 0x0C3365, 0x163871, 0x234678, 0x39608B, 0x163871, 0x163871, 0x3B638C, 0x81B6C9, 0x163871, 0x0C3970, 0x000919,
        0x000C1D, 0x0C3365, 0x163871, 0x294169, 0x123063, 0x163871, 0x123063, 0x163871, 0x123063, 0x163871, 0x0C3970, 0x000A1B,
        0x000A1A, 0x0C488B, 0x2A4AAB, 0x494CCB, 0x4562E2, 0x4562E2, 0x008AD1, 0x048BD1, 0x2780BE, 0x085EA2, 0x0C488B, 0x000A1A,
        0x000C1C, 0x304A52, 0x323D51, 0x233949, 0x2D4051, 0x354655, 0x1B161E, 0x242127, 0x1F3743, 0x0F2028, 0x101724, 0x000C1C,
        0x000F34, 0x1C4451, 0x044C51, 0x0A101F, 0x222340, 0x020825, 0x020925, 0x001B3E, 0x091020, 0x00183D, 0x084351, 0x000F34,
        0x00071F, 0x041C2E, 0x000B28, 0x000B28, 0x242825, 0x01102F, 0x01102F, 0x000C28, 0x000B28, 0x000B28, 0x031B2E, 0x00071F,
    )
    private const val REWARD_TEMPLATE_MAX_DISTANCE = .11
    private const val TEMPLATE_COLS = 12
    private const val TEMPLATE_ROWS = 16
    private val rewardTemplate = intArrayOf(
        0x172B51, 0x172B51, 0x162B51, 0x14284B, 0x122443, 0x374D63, 0x1E304A, 0x1B2D4A, 0x152A4C, 0x172E53, 0x172E52, 0x172D52,
        0x1A3F60, 0x1A3F60, 0x193F60, 0x3E5D6F, 0x3C5B6D, 0x3C515D, 0x16314B, 0x3D5D6F, 0x163755, 0x4A5D6C, 0x616A70, 0x2F4C65,
        0x566A74, 0x32596E, 0x476874, 0x57737A, 0x5B7679, 0x686A69, 0x476489, 0x30596E, 0x61797A, 0x56747A, 0x6A7976, 0x576B73,
        0x30344E, 0x434153, 0x5E757A, 0x576D79, 0x403945, 0x556986, 0x5A80A5, 0x546F75, 0x526975, 0x656A6D, 0x48475C, 0x483F50,
        0x36485E, 0x4B4A3F, 0x726D88, 0x5F5762, 0x695C42, 0x4E71AD, 0x759FD7, 0x335467, 0x31445C, 0x2B425B, 0x395265, 0x323F58,
        0x5D5E84, 0x463F59, 0x494B65, 0x4A4C5C, 0x323C5C, 0x535B7B, 0x535A7B, 0x525569, 0x525469, 0x52556A, 0x5F6283, 0x606382,
        0x445199, 0x213A92, 0x1C357B, 0x172977, 0x37437E, 0x36427E, 0x36427E, 0x37437E, 0x36427E, 0x37427F, 0x45529A, 0x465399,
        0x435894, 0x3B5290, 0x324B90, 0x405694, 0x244B90, 0x405694, 0x13408D, 0x14408D, 0x17418D, 0x425895, 0x435894, 0x435894,
        0x425E97, 0x435F98, 0x3A5C97, 0x315494, 0x395592, 0x124A90, 0x124A90, 0x225093, 0x3E5C97, 0x435E98, 0x425E98, 0x435F98,
        0x2A599F, 0x0B519C, 0x2459A1, 0x3F61A4, 0x3E60A4, 0x3F61A4, 0x4064A3, 0x4064A3, 0x4163A3, 0x4062A4, 0x4162A3, 0x4262A3,
        0x3763AA, 0x3664AE, 0x3564AE, 0x2E62AD, 0x2760AC, 0x1F5DAA, 0x215EAA, 0x3865AE, 0x3B66AF, 0x3864AE, 0x3664AE, 0x275FAB,
        0x191D22, 0x191C22, 0x43565F, 0x765450, 0x653F1C, 0x1E192A, 0x36343B, 0x752D32, 0x73767B, 0x333636, 0x181B22, 0x181B22,
        0x10131E, 0x0A1839, 0x080D1E, 0x070C1C, 0x070B1A, 0x060A19, 0x060A19, 0x070B1A, 0x070C1C, 0x080D1E, 0x0A1839, 0x0A0E1A,
        0x0A132C, 0x090E1F, 0x080E1F, 0x080D1E, 0x10263E, 0x070B1B, 0x0F243B, 0x070C1C, 0x080D1D, 0x080E1F, 0x090E1F, 0x0A1430,
        0x0B142D, 0x0A0F21, 0x0A0F21, 0x0A0F21, 0x0A2045, 0x0A2843, 0x0A2843, 0x0A0C1C, 0x0A0F21, 0x0A0F21, 0x0A0F21, 0x0B1530,
        0x0B142D, 0x0A0F21, 0x0A0F21, 0x0A0F21, 0x0A0F21, 0x0A2843, 0x0A2843, 0x0A0F21, 0x0A0F21, 0x0A0F21, 0x0A0F21, 0x0B1530,
    )

    private fun challengeTemplateDistance(width: Int, height: Int, argbAt: (Int, Int) -> Int) =
        templateDistance(width, height, argbAt, challengeTemplate)

    private fun rewardTemplateDistance(width: Int, height: Int, argbAt: (Int, Int) -> Int) =
        templateDistance(width, height, argbAt, rewardTemplate)

    private fun templateDistance(width: Int, height: Int, argbAt: (Int, Int) -> Int, template: IntArray): Double {
        var difference = 0L
        var index = 0
        for (row in 0 until TEMPLATE_ROWS) for (col in 0 until TEMPLATE_COLS) {
            val x = (width * (col + .5) / TEMPLATE_COLS).toInt().coerceIn(0, width - 1)
            val y = (height * (row + .5) / TEMPLATE_ROWS).toInt().coerceIn(0, height - 1)
            val actual = argbAt(x, y)
            val expected = template[index++]
            difference += kotlin.math.abs((actual shr 16 and 255) - (expected shr 16 and 255))
            difference += kotlin.math.abs((actual shr 8 and 255) - (expected shr 8 and 255))
            difference += kotlin.math.abs((actual and 255) - (expected and 255))
        }
        return difference / (template.size * 3.0 * 255.0)
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
                val p = argbAt(x, y)
                if (match(p shr 16 and 255, p shr 8 and 255, p and 255)) hits++
                total++
            }
        }
        return hits / total.coerceAtLeast(1).toDouble()
    }
}
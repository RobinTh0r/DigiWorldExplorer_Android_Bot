package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenDetectorTest {
    @Test fun locationPinAndRightIconsRecognizeHomeWithoutBattleOrChamber() {
        val source = javax.imageio.ImageIO.read(javaClass.getResource("/home_datagrotto.png"))
        for ((w, h) in listOf(720 to 1280, 1080 to 2160, 1080 to 2340, 1080 to 2400)) {
            for (boxed in listOf(false, true)) {
                val bounds = if (boxed) de.robinthor.digiworldexplorer.vision.GameViewport.fit(w, h)
                    else de.robinthor.digiworldexplorer.vision.GameViewport(0, 0, w, h)
                fun read(x: Int, y: Int, hidePin: Boolean): Int {
                    if (x !in bounds.left until bounds.left + bounds.width || y !in bounds.top until bounds.top + bounds.height) return 0xff000000.toInt()
                    val sx = (x - bounds.left) * source.width / bounds.width
                    val sy = (y - bounds.top) * source.height / bounds.height
                    return if (sy in 470..1150 || (hidePin && sx in 340..380 && sy in 130..165)) 0xff000000.toInt()
                        else source.getRGB(sx, sy)
                }
                assertTrue("icons $w x $h boxed=$boxed", HomeScreenDetector.detect(w, h) { x, y -> read(x, y, false) })
                assertFalse("missing pin $w x $h boxed=$boxed", HomeScreenDetector.detect(w, h) { x, y -> read(x, y, true) })
            }
        }
        org.junit.Assert.assertEquals(EntryScreen.HOME, GameEntryDetector.detect(
            de.robinthor.digiworldexplorer.vision.PixelFrame(source.width, source.height, source::getRGB)).screen)
    }

    @Test fun sharedHeadersAndNavigationOnOtherPagesNeverAuthorizeHomeAcrossFormats() {
        for (name in listOf("bond_partner_grid", "bond_raise_prompt", "farm_explore_live",
            "dungeon_list_bottom", "dungeon_challenge_de", "idle_rewards_empty", "samsung_reward_panel")) {
            val source = javax.imageio.ImageIO.read(javaClass.getResource("/$name.png"))
            for ((w, h) in listOf(720 to 1280, 1080 to 2160, 1080 to 2340, 1080 to 2400)) {
                for (boxed in listOf(false, true)) {
                    val bounds = if (boxed) de.robinthor.digiworldexplorer.vision.GameViewport.fit(w, h)
                        else de.robinthor.digiworldexplorer.vision.GameViewport(0, 0, w, h)
                    assertFalse("$name $w x $h boxed=$boxed", HomeScreenDetector.detect(w, h) { x, y ->
                        if (x !in bounds.left until bounds.left + bounds.width || y !in bounds.top until bounds.top + bounds.height) 0xff000000.toInt()
                        else source.getRGB((x - bounds.left) * source.width / bounds.width,
                            (y - bounds.top) * source.height / bounds.height)
                    })
                }
            }
        }
    }

    @Test fun stageArtworkAndPortraitFormatsDoNotChangeHomeIdentity() {
        val source = javax.imageio.ImageIO.read(javaClass.getResource("/home_dark_city.png"))
        val sizes = listOf(720 to 1280, 1080 to 1920, 1080 to 2160, 1080 to 2340, 1080 to 2400, 1440 to 3200)
        for ((w, h) in sizes) for (boxed in listOf(false, true)) {
            val bounds = if (boxed) de.robinthor.digiworldexplorer.vision.GameViewport.fit(w, h)
                else de.robinthor.digiworldexplorer.vision.GameViewport(0, 0, w, h)
            for (background in listOf(0xff101010.toInt(), 0xffe5e5e5.toInt(), 0xff9220aa.toInt(), 0xff207040.toInt())) {
                val frame = de.robinthor.digiworldexplorer.vision.PixelFrame(w, h) { x, y ->
                    if (x !in bounds.left until bounds.left + bounds.width || y !in bounds.top until bounds.top + bounds.height) 0xff000000.toInt()
                    else {
                        val sx = (x - bounds.left) * source.width / bounds.width
                        val sy = (y - bounds.top) * source.height / bounds.height
                        if (sy in 360..920) background else source.getRGB(sx, sy)
                    }
                }
                org.junit.Assert.assertEquals("$w x $h boxed=$boxed", EntryScreen.HOME, GameEntryDetector.detect(frame).screen)
                org.junit.Assert.assertEquals(bounds, HomeScreenDetector.viewport(w, h, frame::argbAt))
            }
        }
    }

    private val width = 360
    private val height = 640

    @Test fun requiresAllThreeIndependentRegions() {
        assertTrue(HomeScreenDetector.detect(width, height, frame()))
        assertFalse(HomeScreenDetector.detect(width, height, frame(includeNavigation = false)))
        assertFalse(HomeScreenDetector.detect(width, height) { _, _ -> 0xff102a52.toInt() })
    }

    private fun frame(includeNavigation: Boolean = true): (Int, Int) -> Int = { x, y ->
        val fx = x.toDouble() / width
        val fy = y.toDouble() / height
        when {
            includeNavigation && fx in .40.. .60 && fy in .91.. .995 -> 0xff168fd8.toInt()
            fx in .40.. .59 && fy in .745.. .79 -> 0xffe5b040.toInt()
            fx in .42.. .58 && fy in .79.. .86 -> 0xff20c0ee.toInt()
            fx in .05.. .95 && fy in .72.. .92 -> 0xff102a52.toInt()
            fx in .18.. .78 && fy in .14.. .68 -> 0xffe8dfbd.toInt()
            else -> 0xff26384a.toInt()
        }
    }
}

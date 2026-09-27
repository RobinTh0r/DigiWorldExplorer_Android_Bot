package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import javax.imageio.ImageIO

class ScreenTransitionReplayTest {
    @After fun reset() = ScreenDirector.reset()
    private fun frame(name: String): PixelFrame {
        val image = ImageIO.read(javaClass.getResource("/$name.png"))
        return PixelFrame(image.width, image.height, image::getRGB)
    }

    @Test fun `real title loading and ready retain distinct action gates across portrait formats`() {
        for ((name, expected) in listOf("title_loading" to EntryScreen.LOGIN_LOADING, "title_ready" to EntryScreen.LOGIN_READY)) {
            val source = frame(name)
            for ((w, h) in listOf(720 to 1280, 1080 to 1920, 1080 to 2340, 1080 to 2400)) {
                for (boxed in listOf(false, true)) {
                    val v = if (boxed) GameViewport.fit(w, h) else GameViewport(0, 0, w, h)
                    val scaled = PixelFrame(w, h) { x, y ->
                        if (x !in v.left until v.left + v.width || y !in v.top until v.top + v.height) 0xff000000.toInt()
                        else source.argbAt((x-v.left)*source.width/v.width, (y-v.top)*source.height/v.height)
                    }
                    val reading = GameEntryDetector.detect(scaled)
                    assertEquals("$name $w/$h boxed=$boxed", expected, reading.screen)
                    assertEquals(v, reading.viewport)
                }
            }
        }
        val controller = GameEntryController()
        assertEquals(EntryAction.WAIT, controller.tick(GameEntryDetector.detect(frame("title_loading")).screen, 0))
        assertEquals(EntryAction.TOUCH_START, controller.tick(GameEntryDetector.detect(frame("title_ready")).screen, 1000))
    }

    @Test fun `partner rotation replay never becomes login and home remains home during Bond`() {
        val sequence = listOf(
            "home_datagrotto" to ObservedScreen.HOME,
            "bond_partner_collapsed" to ObservedScreen.PARTNER_PAGE,
            "bond_partner_grid" to ObservedScreen.PARTNER_PAGE,
            "bond_partner_selected" to ObservedScreen.PARTNER_PAGE,
            "bond_raise_prompt" to ObservedScreen.MESSAGE,
            "bond_partner_grid" to ObservedScreen.PARTNER_PAGE,
            "home_datagrotto_active" to ObservedScreen.HOME,
            "bond_home_food" to ObservedScreen.HOME)
        for ((name, expected) in sequence) {
            val f = frame(name)
            val entry = GameEntryDetector.detect(f).screen
            assertFalse(name, entry in setOf(EntryScreen.LOGIN_LOADING, EntryScreen.LOGIN_READY))
            repeat(2) { ScreenDirector.observeScreen(PassiveScreenClassifier.detect(f), true) }
            assertEquals(name, expected, ScreenDirector.snapshot().screen)
        }
    }

    @Test fun `reward and exploration replay preserves concrete screens rather than owner labels`() {
        for ((name, expected) in listOf(
            "home_dark_city" to ObservedScreen.HOME,
            "farm_explore_live" to ObservedScreen.EXPLORE_MENU,
            "home_datagrotto" to ObservedScreen.HOME,
            "idle_rewards_empty" to ObservedScreen.IDLE_REWARDS,
            "idle_rewards_result" to ObservedScreen.IDLE_REWARDS,
            "home_dark_city" to ObservedScreen.HOME)) {
            val observed = PassiveScreenClassifier.detect(frame(name))
            repeat(2) { ScreenDirector.observeScreen(observed, true) }
            assertEquals(name, expected, ScreenDirector.snapshot().screen)
        }
    }
}

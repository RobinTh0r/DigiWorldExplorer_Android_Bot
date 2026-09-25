package de.robinthor.digiworldexplorer.automation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenDirectorTest {
    @After fun clean() = ScreenDirector.reset()

    @Test fun requiresTwoMatchingFrames() {
        assertEquals(ObservedScreen.UNKNOWN, ScreenDirector.observe(FrameOwner.FARM, true).screen)
        assertEquals(ObservedScreen.MEAT_FIELD, ScreenDirector.observe(FrameOwner.FARM, true).screen)
    }

    @Test fun animationDoesNotImmediatelyEraseKnownScreen() {
        repeat(2) { ScreenDirector.observe(FrameOwner.GEKKOMON_RUN, true) }
        repeat(5) { assertEquals(ObservedScreen.GEKKOMON_RUN, ScreenDirector.observe(FrameOwner.NONE, true).screen) }
        assertEquals(ObservedScreen.UNKNOWN, ScreenDirector.observe(FrameOwner.NONE, true).screen)
    }

    @Test fun reportsActionAndSafeState() {
        ScreenDirector.noteAction("  Waiting   for result ")
        val snapshot = ScreenDirector.observe(FrameOwner.NONE, false)
        assertEquals("Automation off", snapshot.state)
        assertEquals("Waiting for result", snapshot.action)
    }

    @Test fun foreignActionCannotOverwriteProvenFarmAction() {
        repeat(2) { ScreenDirector.observeScreen(ObservedScreen.MEAT_FIELD, true) }
        ScreenDirector.noteAction("Farm checking", ObservedScreen.MEAT_FIELD)
        ScreenDirector.noteAction("AUTO: VS / Tower", ObservedScreen.DUNGEON)
        assertEquals("Farm checking", ScreenDirector.snapshot().action)
    }

    @Test fun changingScreenClearsStaleAction() {
        repeat(2) { ScreenDirector.observeScreen(ObservedScreen.DUNGEON, true) }
        ScreenDirector.noteAction("AUTO: VS / Tower", ObservedScreen.DUNGEON)
        ScreenDirector.observeScreen(ObservedScreen.MEAT_FIELD, true)
        val snapshot = ScreenDirector.observeScreen(ObservedScreen.MEAT_FIELD, true)
        assertEquals(ObservedScreen.MEAT_FIELD, snapshot.screen)
        assertEquals("No action", snapshot.action)
    }
}

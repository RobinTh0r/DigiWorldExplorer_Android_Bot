package de.robinthor.digiworldexplorer.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class BotEyeStateResolverTest {
    @Test fun `off closes the eyes`() = assertEquals(
        BotEyeState.OFF,
        BotEyeStateResolver.resolve(DirectorSnapshot(state = "Automation off")),
    )

    @Test fun `recognized active screen is green state`() = assertEquals(
        BotEyeState.ACTIVE,
        BotEyeStateResolver.resolve(DirectorSnapshot(ObservedScreen.HOME, "Active", "No action", 100)),
    )

    @Test fun `candidate and explicit scan are search state`() {
        assertEquals(BotEyeState.SEARCHING, BotEyeStateResolver.resolve(DirectorSnapshot(state = "Watching", confidence = 50)))
        assertEquals(BotEyeState.SEARCHING, BotEyeStateResolver.resolve(DirectorSnapshot(state = "Watching", action = "Checking screen")))
    }

    @Test fun `settled unknown is gray state`() = assertEquals(
        BotEyeState.UNKNOWN,
        BotEyeStateResolver.resolve(DirectorSnapshot(state = "Watching", action = "No action", confidence = 0)),
    )

    @Test fun `unsafe and capture failures are error state`() {
        assertEquals(BotEyeState.ERROR, BotEyeStateResolver.resolve(DirectorSnapshot(state = "Active", action = "Dungeon screen not confirmed")))
        assertEquals(BotEyeState.ERROR, BotEyeStateResolver.resolve(DirectorSnapshot(ObservedScreen.CAPTURE_BLOCKED, "Paused")))
    }
}

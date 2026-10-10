package de.robinthor.digiworldexplorer.accessibility

import org.junit.Assert.*
import org.junit.Test

class QuickOverlayPresentationTest {
    @Test fun taskCardIsHiddenForCopilotAndDungeonRegardlessOfIdlePreference() {
        for(preference in listOf(false,true)) {
            assertFalse(showDirectorCard(preference,false,true))
            assertFalse(showDirectorCard(preference,true,false))
            assertFalse(showDirectorCard(preference,true,true))
        }
    }
    @Test fun idleCardUsesTheUnchangedUserPreferenceAgain() {
        assertTrue(showDirectorCard(true,false,false))
        assertFalse(showDirectorCard(false,false,false))
    }
}

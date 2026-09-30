package de.robinthor.digiworldexplorer.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DwsNavigationProfileTest {
    @Test fun v3RestoresClassicRules() {
        val settings = DwsNavigationProfile.V3_CLASSIC.settings()
        assertTrue(settings.allowLeft)
        assertFalse(settings.forceForwardAttack)
        assertFalse(settings.dashSpamUntilZero)
        assertFalse(settings.collectOnlyEnergy)
        assertFalse(settings.trackAllSprites)
        assertFalse(settings.phoneSafeMovement)
    }

    @Test fun v4UsesLegacySpriteDetectionAndDashRhythm() {
        val settings = DwsNavigationProfile.V4_DASH.settings()
        assertFalse(settings.allowLeft)
        assertTrue(settings.forceForwardAttack)
        assertTrue(settings.dashSpamUntilZero)
        assertTrue(settings.collectOnlyEnergy)
        assertFalse(settings.trackAllSprites)
        assertFalse(settings.phoneSafeMovement)
    }

    @Test fun v5CombinesAllSpritesDashAndPhoneFixes() {
        val settings = DwsNavigationProfile.V5_ALL_SPRITES.settings()
        assertFalse(settings.allowLeft)
        assertTrue(settings.forceForwardAttack)
        assertTrue(settings.dashSpamUntilZero)
        assertTrue(settings.collectOnlyEnergy)
        assertTrue(settings.trackAllSprites)
        assertTrue(settings.phoneSafeMovement)
    }

    @Test fun preferenceDefaultsToV4Dash() {
        assertEquals(DwsNavigationProfile.V4_DASH, DwsNavigationProfile.fromPreference("V4_DASH"))
        assertEquals(DwsNavigationProfile.V4_DASH, DwsNavigationProfile.fromPreference(null))
        assertEquals(DwsNavigationProfile.V4_DASH, DwsNavigationProfile.fromPreference("unknown"))
    }
}

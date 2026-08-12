package de.robinthor.digiworldexplorer.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertEquals

class UpdateCheckerTest {
    @Test fun detectsNewMajorVersion() = assertTrue(UpdateChecker.isNewer("0.3.0-rc1", "v1.0.0"))
    @Test fun acceptsSameVersion() = assertFalse(UpdateChecker.isNewer("1.0.0", "v1.0.0"))
    @Test fun stableIsNewerThanReleaseCandidate() = assertTrue(UpdateChecker.isNewer("1.0.0-rc1", "v1.0.0"))
    @Test fun olderReleaseIsNotAnUpdate() = assertFalse(UpdateChecker.isNewer("1.0.0", "v0.3.0"))
    @Test fun newerBetaNumberIsDetected() = assertTrue(UpdateChecker.isNewer("2.3.0-beta.1", "v2.3.0-beta.2"))
    @Test fun olderBetaNumberIsRejected() = assertFalse(UpdateChecker.isNewer("2.3.0-beta.2", "v2.3.0-beta.1"))
    @Test fun betaDoesNotReplaceSameStableVersion() = assertFalse(UpdateChecker.isNewer("2.3.0", "v2.3.0-beta.9"))
    @Test fun newerPatchBetaIsDetectedByOldBeta() = assertTrue(UpdateChecker.isNewer("2.3.0-beta.1", "v2.3.1-beta.1"))
    @Test fun optionalPreReleaseCanBeSelected() {
        val releases=listOf(UpdateChecker.ReleaseInfo("v2.3.0-beta.1","beta",true,false),UpdateChecker.ReleaseInfo("v2.2.0","stable",false,false))
        assertEquals("v2.3.0-beta.1",UpdateChecker.selectRelease(releases,true)?.tag)
        assertEquals("v2.2.0",UpdateChecker.selectRelease(releases,false)?.tag)
    }
    @Test fun draftsAreNeverSelected() {
        val releases=listOf(UpdateChecker.ReleaseInfo("v9.0.0","draft",true,true),UpdateChecker.ReleaseInfo("v2.3.0-beta.1","beta",true,false))
        assertEquals("v2.3.0-beta.1",UpdateChecker.selectRelease(releases,true)?.tag)
    }}
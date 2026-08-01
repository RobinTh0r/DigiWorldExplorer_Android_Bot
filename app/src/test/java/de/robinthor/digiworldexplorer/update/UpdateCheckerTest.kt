package de.robinthor.digiworldexplorer.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun detectsNewMajorVersion() = assertTrue(UpdateChecker.isNewer("0.3.0-rc1", "v1.0.0"))
    @Test fun acceptsSameVersion() = assertFalse(UpdateChecker.isNewer("1.0.0", "v1.0.0"))
    @Test fun stableIsNewerThanReleaseCandidate() = assertTrue(UpdateChecker.isNewer("1.0.0-rc1", "v1.0.0"))
    @Test fun olderReleaseIsNotAnUpdate() = assertFalse(UpdateChecker.isNewer("1.0.0", "v0.3.0"))
}
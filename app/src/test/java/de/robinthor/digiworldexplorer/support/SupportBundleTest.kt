package de.robinthor.digiworldexplorer.support

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class SupportBundleTest {
    @Test fun `export excludes unknown and sensitive preference keys`() {
        val report = SupportBundle.report(mapOf("model" to "Test Device", "license" to "SECRET", "email" to "PRIVATE"), emptyList())
        assertTrue(report.contains("model=Test Device"))
        assertFalse(report.contains("SECRET"))
        assertFalse(report.contains("PRIVATE"))
    }

    @Test fun `zip contains exactly the reviewed report in utf8`() {
        val report = "Überprüfung ✓"
        ZipInputStream(ByteArrayInputStream(SupportBundle.zip(report))).use { zip ->
            assertEquals("support-report.txt", zip.nextEntry.name)
            assertEquals(report, zip.readBytes().toString(Charsets.UTF_8))
            assertNull(zip.nextEntry)
        }
    }
}

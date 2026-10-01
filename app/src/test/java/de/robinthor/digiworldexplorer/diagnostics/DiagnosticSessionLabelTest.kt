package de.robinthor.digiworldexplorer.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticSessionLabelTest {
    @Test fun `session folder becomes readable local date and time`() {
        assertEquals("01.10.2026 · 19:18:43", diagnosticSessionLabel("diagnostic-20261001-191843"))
    }

    @Test fun `unexpected folder name remains readable`() {
        assertEquals("manual", diagnosticSessionLabel("diagnostic-manual"))
    }
}

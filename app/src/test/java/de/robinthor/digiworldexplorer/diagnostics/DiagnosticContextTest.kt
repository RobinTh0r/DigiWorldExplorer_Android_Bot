package de.robinthor.digiworldexplorer.diagnostics

import org.junit.Assert.*
import org.junit.Test

class DiagnosticContextTest {
    @Test fun snapshotKeepsAdGateAndCardValuesButNeverReadsActivationOrAccountData() {
        val requested = mutableSetOf<String>()
        val saved = mapOf<String, Any>("ad_skip_pass" to true, "dungeon_use_ads" to false,
            "dungeon_card_digifactory" to false, "supporter_code" to "secret", "account_id" to "private")
        val snapshot = collectDiagnosticSettings { key, default ->
            requested += key
            saved[key] ?: default
        }
        assertEquals(true, snapshot["ad_skip_pass"])
        assertEquals(false, snapshot["dungeon_use_ads"])
        assertEquals(false, snapshot["dungeon_card_digifactory"])
        assertEquals(3, snapshot["dungeon_normal_attempts"])
        assertFalse(requested.contains("supporter_code"))
        assertFalse(requested.contains("account_id"))
        assertFalse(snapshot.values.contains("secret"))
    }
}

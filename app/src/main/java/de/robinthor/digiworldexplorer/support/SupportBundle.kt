package de.robinthor.digiworldexplorer.support

import de.robinthor.digiworldexplorer.automation.AutomationEvent
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Only explicit diagnostics may enter an export, never an entire preference store. */
object SupportBundle {
    private val allowedKeys = setOf(
        "appVersion", "androidApi", "manufacturer", "model", "displayPixels", "language",
        "timezone", "mode", "automationEnabled", "runnerEnabled", "dungeonEnabled",
        "bondEnabled", "summonEnabled", "networkEnabled", "farmHarvestEnabled",
    )

    fun report(metadata: Map<String, String>, events: List<AutomationEvent>): String = buildString {
        appendLine("DigiWorldExplorer support report / schema 1")
        appendLine("No screenshots, account identifiers, credentials or license keys are collected.")
        appendLine("Review device details before sharing this file.")
        appendLine()
        metadata.filterKeys { it in allowedKeys }.toSortedMap().forEach { (key, value) ->
            appendLine("$key=${value.replace('\n', ' ').replace('\r', ' ').take(160)}")
        }
        appendLine()
        appendLine("Events (milliseconds since device boot; latest 200):")
        events.takeLast(200).forEach { event ->
            // Accept only structured codes even if a caller constructs an event outside the ring.
            val code = event.code.take(80).filter { it.isLetterOrDigit() || it in "_:-." }
            appendLine("${event.elapsedMillis}\t${event.kind.name}\t$code")
        }
    }

    fun zip(reviewedReport: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("support-report.txt"))
            zip.write(reviewedReport.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }
}

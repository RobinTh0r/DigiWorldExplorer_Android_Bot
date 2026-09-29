package de.robinthor.digiworldexplorer.diagnostics

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiagnosticQuickControls(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(PersistentDiagnosticLog.isEnabled(context)) }
    var sessions by remember { mutableStateOf(PersistentDiagnosticLog.sessions(context)) }
    var showFiles by remember { mutableStateOf(false) }
    fun refresh() { sessions = PersistentDiagnosticLog.sessions(context) }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        OutlinedButton(
            onClick = { enabled = !enabled; PersistentDiagnosticLog.setEnabled(context, enabled); refresh() },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 3.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        ) { Text(if (enabled) "● Diagnose" else "○ Diagnose", fontSize = 10.sp, maxLines = 1) }
        OutlinedButton(onClick = { refresh(); showFiles = true }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp)) {
            Text("≡", fontSize = 17.sp)
        }
        OutlinedButton(onClick = {
            refresh()
            val share = PersistentDiagnosticLog.shareAllIntent(context)
            if (share == null) Toast.makeText(context, "Keine Diagnosen gespeichert", Toast.LENGTH_SHORT).show()
            else context.startActivity(Intent.createChooser(share, "Alle Diagnose-ZIPs teilen"))
        }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)) { Text("⇧ All", fontSize = 10.sp, maxLines = 1) }
    }

    if (showFiles) AlertDialog(
        onDismissRequest = { showFiles = false },
        title = { Text("Diagnosedateien") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sessions.isEmpty()) Text("Keine Diagnosen gespeichert")
                sessions.forEach { session ->
                    Column {
                        Text(session.name.removePrefix("diagnostic-"), style = MaterialTheme.typography.labelLarge)
                        Text("${(session.bytes / 1024).coerceAtLeast(1)} KB", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TextButton(onClick = { context.startActivity(Intent.createChooser(PersistentDiagnosticLog.shareIntent(context, session), "Diagnose-ZIP teilen")) }) { Text("Teilen") }
                            TextButton(onClick = { PersistentDiagnosticLog.delete(session); refresh() }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = { showFiles = false }) { Text("Schließen") } },
    )
}

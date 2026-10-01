package de.robinthor.digiworldexplorer.diagnostics

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Button(
            onClick = { enabled = !enabled; PersistentDiagnosticLog.setEnabled(context, enabled); refresh() },
            modifier = Modifier.weight(1.35f).heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (enabled) Color(0xFF168A3A) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) { Text(if (enabled) "● Diagnose läuft" else "○ Diagnose", fontSize = 11.sp, maxLines = 1) }
        OutlinedButton(onClick = { refresh(); showFiles = true }, modifier = Modifier.weight(.55f).heightIn(min = 44.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
            Text("≡", fontSize = 22.sp)
        }
        OutlinedButton(onClick = {
            refresh()
            val share = PersistentDiagnosticLog.shareAllIntent(context)
            if (share == null) Toast.makeText(context, "Keine Diagnosen gespeichert", Toast.LENGTH_SHORT).show()
            else context.startActivity(Intent.createChooser(share, "Alle Diagnose-ZIPs teilen"))
        }, modifier = Modifier.weight(.9f).heightIn(min = 44.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)) { Text("⇧ Alle", fontSize = 12.sp, maxLines = 1) }
    }

    if (showFiles) AlertDialog(
        onDismissRequest = { showFiles = false },
        title = { Text("Diagnosedateien") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sessions.isEmpty()) Text("Keine Diagnosen gespeichert")
                sessions.forEach { session ->
                    Column {
                        Text(diagnosticSessionLabel(session.name), style = MaterialTheme.typography.labelLarge)
                        Text("${(session.bytes / 1024).coerceAtLeast(1)} KB", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TextButton(onClick = { context.startActivity(Intent.createChooser(PersistentDiagnosticLog.shareIntent(context, session), "Diagnose-ZIP teilen")) }) { Text("Teilen") }
                            TextButton(onClick = { PersistentDiagnosticLog.delete(session); refresh() }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    HorizontalDivider()
                }
                if (sessions.isNotEmpty()) OutlinedButton(onClick = {
                    val share = PersistentDiagnosticLog.shareAllIntent(context)
                    if (share != null) context.startActivity(Intent.createChooser(share, "Alle Diagnose-ZIPs teilen"))
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) { Text("Alle Diagnosen teilen") }
            }
        },
        confirmButton = { TextButton(onClick = { showFiles = false }) { Text("Schließen") } },
    )
}

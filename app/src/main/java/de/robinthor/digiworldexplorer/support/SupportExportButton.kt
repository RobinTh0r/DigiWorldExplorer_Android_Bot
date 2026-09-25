package de.robinthor.digiworldexplorer.support

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.robinthor.digiworldexplorer.BuildConfig
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.automation.AutomationEventLog
import de.robinthor.digiworldexplorer.automation.AutomationMode
import de.robinthor.digiworldexplorer.strategy.AutomationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

@Composable
fun SupportExportButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val reviewed = report
        if (uri == null || reviewed == null) {
            exporting = false
        } else {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Document unavailable")
                        output.use { it.write(SupportBundle.zip(reviewed)) }
                    }.isSuccess
                }
                exporting = false
                Toast.makeText(context, if (success) R.string.support_saved else R.string.support_save_failed, Toast.LENGTH_LONG).show()
            }
        }
    }
    OutlinedButton(enabled = !exporting, onClick = {
        val metrics = context.resources.displayMetrics
        report = SupportBundle.report(mapOf(
            "appVersion" to BuildConfig.VERSION_NAME,
            "androidApi" to Build.VERSION.SDK_INT.toString(),
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "displayPixels" to "${metrics.widthPixels}x${metrics.heightPixels}",
            "language" to context.resources.configuration.locales[0].toLanguageTag(),
            "timezone" to TimeZone.getDefault().id,
            "mode" to if (AutomationState.mode == AutomationMode.SEMI_AUTO) "Co-Pilot" else "Expedition (preview)",
            "automationEnabled" to AutomationState.enabled.toString(),
            "runnerEnabled" to AutomationState.autoRunnerEnabled.toString(),
            "farmHarvestEnabled" to AutomationState.autoFarmEnabled.toString(),
            "dungeonEnabled" to AutomationState.autoDungeonEnabled.toString(),
            "bondEnabled" to AutomationState.autoFeedEnabled.toString(),
            "summonEnabled" to AutomationState.autoPurchaseEnabled.toString(),
            "networkEnabled" to AutomationState.autoNetworkDefenseEnabled.toString(),
        ), AutomationEventLog.snapshot())
        preview = true
    }) { Text(stringResource(R.string.support_export)) }
    if (preview) AlertDialog(
        onDismissRequest = { preview = false },
        title = { Text(stringResource(R.string.support_preview)) },
        text = { Text(report.orEmpty(), Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = {
            preview = false
            exporting = true
            launcher.launch("DigiWorldExplorer-support.zip")
        }) { Text(stringResource(R.string.support_save)) } },
        dismissButton = { TextButton(onClick = { preview = false }) { Text(stringResource(R.string.close)) } },
    )
}

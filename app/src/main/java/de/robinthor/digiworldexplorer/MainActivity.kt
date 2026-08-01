package de.robinthor.digiworldexplorer

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.update.UpdateChecker
import de.robinthor.digiworldexplorer.update.UpdateResult
import java.util.Locale

private enum class UiStatus { READY, CAPTURING, AUTOMATIC, CAPTURE_DENIED, STOPPED }
private enum class UpdateStatus { CHECKING, CURRENT, AVAILABLE, FAILED }

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf(UiStatus.READY)
    private var capture by mutableStateOf(false)
    private var auto by mutableStateOf(false)
    private var grid by mutableStateOf(true)
    private var access by mutableStateOf(false)
    private var overlay by mutableStateOf(false)
    private var updateStatus by mutableStateOf(UpdateStatus.CHECKING)
    private var updateVersion by mutableStateOf("")
    private var updateUrl by mutableStateOf("")

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            AutomationState.stop()
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            capture = true; auto = false; status = UiStatus.CAPTURING
        } else status = UiStatus.CAPTURE_DENIED
    }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun attachBaseContext(base: Context) {
        if (Build.VERSION.SDK_INT < 33) {
            val tag = base.getSharedPreferences("settings", MODE_PRIVATE).getString("language", null)
            if (tag != null) {
                val config = base.resources.configuration
                config.setLocale(Locale.forLanguageTag(tag))
                super.attachBaseContext(base.createConfigurationContext(config)); return
            }
        }
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutomationState.stop()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) {
            ControlScreen(
                status, capture, auto, grid, access, overlay, updateStatus, updateVersion,
                onAccess = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                onOverlay = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                onGrid = { grid = !grid; AutomationState.overlayEnabled = grid; DigiWorldAccessibilityService.instance?.setOverlayEnabled(grid) },
                onCapture = { consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()) },
                onStart = { if (capture && access) { ScreenCaptureService.setAutomation(this, true); auto = true; status = UiStatus.AUTOMATIC } },
                onStop = { ScreenCaptureService.setAutomation(this, false); auto = false; status = UiStatus.CAPTURING },
                onAll = { ScreenCaptureService.stop(this); capture = false; auto = false; status = UiStatus.STOPPED },
                onLanguage = ::setLanguage,
                onCheckUpdate = ::checkForUpdates,
                onOpenUpdate = { if (updateUrl.isNotBlank()) openUrl(updateUrl) },
                onDonate = { openUrl(getString(R.string.donate_url)) },
                onRepo = { openUrl(getString(R.string.footer_repo_url)) },
            )
        } } }
        checkForUpdates()
    }

    private fun checkForUpdates() {
        updateStatus = UpdateStatus.CHECKING
        UpdateChecker.check(BuildConfig.VERSION_NAME) { result -> runOnUiThread {
            when (result) {
                UpdateResult.Current -> updateStatus = UpdateStatus.CURRENT
                UpdateResult.Failed -> updateStatus = UpdateStatus.FAILED
                is UpdateResult.Available -> { updateStatus = UpdateStatus.AVAILABLE; updateVersion = result.version; updateUrl = result.url }
            }
        } }
    }

    private fun openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    private fun setLanguage(tag: String) {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", tag).apply()
        if (Build.VERSION.SDK_INT >= 33) getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        else { val config = resources.configuration; config.setLocale(Locale.forLanguageTag(tag)); resources.updateConfiguration(config, resources.displayMetrics); recreate() }
    }

    override fun onResume() {
        super.onResume()
        access = (getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            it.resolveInfo.serviceInfo.packageName == packageName && it.resolveInfo.serviceInfo.name == DigiWorldAccessibilityService::class.java.name
        }
        overlay = Settings.canDrawOverlays(this)
    }
}

@Composable private fun ControlScreen(status: UiStatus, capture: Boolean, auto: Boolean, grid: Boolean, access: Boolean, overlay: Boolean, update: UpdateStatus, updateVersion: String, onAccess: () -> Unit, onOverlay: () -> Unit, onGrid: () -> Unit, onCapture: () -> Unit, onStart: () -> Unit, onStop: () -> Unit, onAll: () -> Unit, onLanguage: (String) -> Unit, onCheckUpdate: () -> Unit, onOpenUpdate: () -> Unit, onDonate: () -> Unit, onRepo: () -> Unit) {
    val statusText = when (status) { UiStatus.READY -> R.string.status_ready; UiStatus.CAPTURING -> R.string.status_capture; UiStatus.AUTOMATIC -> R.string.status_auto; UiStatus.CAPTURE_DENIED -> R.string.status_capture_denied; UiStatus.STOPPED -> R.string.status_stopped }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(stringResource(R.string.app_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall) }
            Text(if (auto) "●" else "○")
        }
        UpdateCard(update, updateVersion, onCheckUpdate, onOpenUpdate)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(stringResource(R.string.status_title), fontWeight = FontWeight.Bold); Text(stringResource(statusText)) } }
        Text(stringResource(R.string.setup_title), fontWeight = FontWeight.Bold)
        PermissionButton(1, R.string.permission_accessibility, access, onAccess)
        PermissionButton(2, R.string.permission_overlay, overlay, onOverlay)
        Button(onClick = onCapture, Modifier.fillMaxWidth()) { Text(stringResource(if (capture) R.string.capture_renew else R.string.capture_start)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onGrid, enabled = overlay, modifier = Modifier.weight(1f)) { Text(stringResource(if (grid) R.string.grid_hide else R.string.grid_show)) }
            Button(onClick = onStart, enabled = capture && access && !auto, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.auto_start)) }
        }
        Button(onClick = onStop, enabled = auto, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.auto_stop)) }
        OutlinedButton(onClick = onAll, Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop_all)) }
        Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onDonate, Modifier.fillMaxWidth()) { Text(stringResource(R.string.donate)) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRepo, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.source_code)) }
            TextButton(onClick = { onLanguage("de") }) { Text("🇩🇪 DE") }
            TextButton(onClick = { onLanguage("en") }) { Text("🇬🇧 EN") }
        }
    }
}

@Composable private fun UpdateCard(status: UpdateStatus, version: String, onCheck: () -> Unit, onOpen: () -> Unit) {
    val text = when (status) { UpdateStatus.CHECKING -> stringResource(R.string.update_checking); UpdateStatus.CURRENT -> stringResource(R.string.update_current); UpdateStatus.AVAILABLE -> stringResource(R.string.update_available, version); UpdateStatus.FAILED -> stringResource(R.string.update_failed) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (status == UpdateStatus.AVAILABLE) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = if (status == UpdateStatus.AVAILABLE) onOpen else onCheck) { Text(stringResource(if (status == UpdateStatus.AVAILABLE) R.string.update_download else R.string.update_check)) }
        }
    }
}

@Composable private fun PermissionButton(number: Int, label: Int, granted: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, Modifier.fillMaxWidth()) { Text("$number. ${stringResource(label)}", Modifier.weight(1f)); Text(stringResource(if (granted) R.string.permission_granted else R.string.permission_missing), fontWeight = FontWeight.Bold) }
}
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    private var autoPurchase by mutableStateOf(true)
    private var autoDungeon by mutableStateOf(true)
    private var legacyCapture by mutableStateOf(false)
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
        autoPurchase = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_purchase", true)
        autoDungeon = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_dungeon", true)
        legacyCapture = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("legacy_capture", false)
        AutomationState.autoPurchaseEnabled = autoPurchase
        AutomationState.autoDungeonEnabled = autoDungeon
        AutomationState.forceLegacyCaptureMetrics = legacyCapture
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) {
            ControlScreen(
                status, capture, auto, grid, autoPurchase, autoDungeon, legacyCapture, access, overlay, updateStatus, updateVersion,
                onAccess = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                onOverlay = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                onGrid = { grid = !grid; AutomationState.overlayEnabled = grid; DigiWorldAccessibilityService.instance?.setOverlayEnabled(grid) },
                onAutoPurchase = { enabled -> autoPurchase = enabled; AutomationState.autoPurchaseEnabled = enabled; getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("auto_purchase", enabled).apply() },
                onAutoDungeon = { enabled -> autoDungeon = enabled; AutomationState.autoDungeonEnabled = enabled; getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("auto_dungeon", enabled).apply() },
                onLegacyCapture = { enabled -> legacyCapture = enabled; AutomationState.forceLegacyCaptureMetrics = enabled; getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("legacy_capture", enabled).apply() },
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

@Composable private fun ControlScreen(status: UiStatus, capture: Boolean, auto: Boolean, grid: Boolean, autoPurchase: Boolean, autoDungeon: Boolean, legacyCapture: Boolean, access: Boolean, overlay: Boolean, update: UpdateStatus, updateVersion: String, onAccess: () -> Unit, onOverlay: () -> Unit, onGrid: () -> Unit, onAutoPurchase: (Boolean) -> Unit, onAutoDungeon: (Boolean) -> Unit, onLegacyCapture: (Boolean) -> Unit, onCapture: () -> Unit, onStart: () -> Unit, onStop: () -> Unit, onAll: () -> Unit, onLanguage: (String) -> Unit, onCheckUpdate: () -> Unit, onOpenUpdate: () -> Unit, onDonate: () -> Unit, onRepo: () -> Unit) {
    var showAccessHelp by remember { mutableStateOf(false) }
    var featureHelp by remember { mutableStateOf<Int?>(null) }
    val statusText = when (status) { UiStatus.READY -> R.string.status_ready; UiStatus.CAPTURING -> R.string.status_capture; UiStatus.AUTOMATIC -> R.string.status_auto; UiStatus.CAPTURE_DENIED -> R.string.status_capture_denied; UiStatus.STOPPED -> R.string.status_stopped }
    if (showAccessHelp) AlertDialog(onDismissRequest = { showAccessHelp = false }, title = { Text(stringResource(R.string.accessibility_help_title)) }, text = { Text(stringResource(R.string.accessibility_help_body)) }, confirmButton = { TextButton(onClick = { showAccessHelp = false }) { Text(stringResource(R.string.close)) } })
    featureHelp?.let { FeatureHelpDialog(it, onClose = { featureHelp = null }) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(stringResource(R.string.app_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall) }
            Text(if (auto) "●" else "○")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactStatusCard(statusText, Modifier.weight(1.15f))
            CompactUpdateCard(update, updateVersion, onCheckUpdate, onOpenUpdate, Modifier.weight(.85f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAccessHelp = true }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(stringResource(R.string.accessibility_blocked_help), style = MaterialTheme.typography.labelSmall) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactPermissionButton(1, R.string.permission_accessibility, access, onAccess, Modifier.weight(1f))
            CompactPermissionButton(2, R.string.permission_overlay, overlay, onOverlay, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.legacy_capture))
                Text(stringResource(R.string.legacy_capture_hint), style = MaterialTheme.typography.labelSmall)
            }
            Switch(checked = legacyCapture, onCheckedChange = onLegacyCapture)
        }
        Button(onClick = onCapture, Modifier.fillMaxWidth()) { Text(stringResource(if (capture) R.string.capture_renew else R.string.capture_start)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onGrid, enabled = overlay, modifier = Modifier.weight(1f)) { Text(stringResource(if (grid) R.string.grid_hide else R.string.grid_show)) }
            Button(onClick = onStart, enabled = capture && access && !auto, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.auto_start)) }
        }
        FeatureSwitch(R.string.auto_purchase, autoPurchase, onAutoPurchase, onHelp = { featureHelp = 0 })
        FeatureSwitch(R.string.auto_dungeon, autoDungeon, onAutoDungeon, onHelp = { featureHelp = 1 })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onStop, enabled = auto, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), contentPadding = PaddingValues(horizontal = 6.dp)) { Text(stringResource(R.string.auto_stop), style = MaterialTheme.typography.labelMedium) }
            OutlinedButton(onClick = onAll, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text(stringResource(R.string.stop_all), style = MaterialTheme.typography.labelMedium) }
        }
        Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.labelSmall)
        OutlinedButton(onClick = onDonate, Modifier.fillMaxWidth()) { Text(stringResource(R.string.donate)) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRepo, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp)) { Text(stringResource(R.string.source_code), style = MaterialTheme.typography.labelSmall) }
            TextButton(onClick = { onLanguage("de") }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("🇩🇪 DE") }
            TextButton(onClick = { onLanguage("en") }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("🇬🇧 EN") }
        }
    }
}

@Composable private fun CompactStatusCard(statusText: Int, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) { Text(stringResource(R.string.status_title), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold); Text(stringResource(statusText), style = MaterialTheme.typography.labelSmall, maxLines = 2) } }
}

@Composable private fun CompactUpdateCard(status: UpdateStatus, version: String, onCheck: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val text = when (status) { UpdateStatus.CHECKING -> stringResource(R.string.update_checking); UpdateStatus.CURRENT -> stringResource(R.string.update_current); UpdateStatus.AVAILABLE -> stringResource(R.string.update_available, version); UpdateStatus.FAILED -> stringResource(R.string.update_failed) }
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (status == UpdateStatus.AVAILABLE) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 2)
            TextButton(onClick = if (status == UpdateStatus.AVAILABLE) onOpen else onCheck, contentPadding = PaddingValues(3.dp)) { Text(if (status == UpdateStatus.AVAILABLE) "↓" else "↻", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable private fun CompactPermissionButton(number: Int, label: Int, granted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
        Text("$number. ${stringResource(label)}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 2)
        Text(if (granted) "✓" else "○", fontWeight = FontWeight.Bold)
    }
}

@Composable private fun FeatureSwitch(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit, onHelp: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", fontWeight = FontWeight.Bold) }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable private fun FeatureHelpDialog(kind: Int, onClose: () -> Unit) {
    val summon = kind == 0
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(if (summon) R.string.summon_help_title else R.string.dungeon_help_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(if (summon) R.string.summon_help_body else R.string.dungeon_help_body))
                Image(
                    painter = painterResource(if (summon) R.drawable.help_auto_summon else R.drawable.help_vs_dungeon),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}
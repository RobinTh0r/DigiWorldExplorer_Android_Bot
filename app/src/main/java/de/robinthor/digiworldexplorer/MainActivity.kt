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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.CaptureSessionState
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer
import de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer
import de.robinthor.digiworldexplorer.license.SupporterLicense
import de.robinthor.digiworldexplorer.license.SupporterLicenseManager
import de.robinthor.digiworldexplorer.strategy.AutomationState
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.DwsNavigationSettings
import de.robinthor.digiworldexplorer.update.UpdateChecker
import de.robinthor.digiworldexplorer.update.UpdateResult
import java.util.Locale

private enum class UiStatus { READY, CAPTURING, AUTOMATIC, CAPTURE_DENIED, STOPPED }
private enum class UpdateStatus { CHECKING, CURRENT, AVAILABLE, FAILED }

class MainActivity : ComponentActivity() {
    private companion object {
        const val GAME_PACKAGE = "com.bandainamcoent.dgup_ww"
        fun isHuaweiOrHonor() = listOf(Build.MANUFACTURER, Build.BRAND).any {
            it.equals("HUAWEI", ignoreCase = true) || it.equals("HONOR", ignoreCase = true)
        }
    }
    private var status by mutableStateOf(UiStatus.READY)
    private var capture by mutableStateOf(false)
    private var auto by mutableStateOf(false)
    private var grid by mutableStateOf(true)
    private var autoPurchase by mutableStateOf(true)
    private var autoDungeon by mutableStateOf(true)
    private var autoNetworkDefense by mutableStateOf(false)
    private var autoFeed by mutableStateOf(false)
    private var dwsNeverLeft by mutableStateOf(false)
    private var dwsForceForwardAttack by mutableStateOf(false)
    private var dwsDashSpam by mutableStateOf(false)
    private var dwsOnlyEnergy by mutableStateOf(false)
    private var dwsBetterCollect by mutableStateOf(false)
    private var dwsBlindStageTap by mutableStateOf(false)
    private var showSupportPrompt by mutableStateOf(false)
    private var legacyCapture by mutableStateOf(false)
    private var summonTouchCorrection by mutableStateOf(false)
    private var preReleaseUpdates by mutableStateOf(false)
    private var supporterLicense by mutableStateOf<SupporterLicense?>(null)
    private var showLicenseDialog by mutableStateOf(false)
    private var showReleaseNotes by mutableStateOf(false)
    private var showCommunityIntro by mutableStateOf(false)
    private var showUpdateNotice by mutableStateOf(false)
    private var showFeedDelayNotice by mutableStateOf(false)
    private var access by mutableStateOf(false)
    private var overlay by mutableStateOf(false)
    private var updateStatus by mutableStateOf(UpdateStatus.CHECKING)
    private var updateVersion by mutableStateOf("")
    private var updateUrl by mutableStateOf("")
    private var startAfterCapture = false

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            AutomationState.stop()
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            capture = true; auto = false; status = UiStatus.CAPTURING
            if (startAfterCapture) {
                startAfterCapture = false
                continueAutomationStart()
            }
        } else {
            startAfterCapture = false
            status = UiStatus.CAPTURE_DENIED
        }
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
        val settings = getSharedPreferences("settings", MODE_PRIVATE)
        val releaseNotesKey = "release_notes_${BuildConfig.VERSION_NAME}"
        val communityIntroKey = "discord_community_intro_v1"
        showReleaseNotes = !settings.getBoolean(releaseNotesKey, false)
        showCommunityIntro = !showReleaseNotes && !settings.getBoolean(communityIntroKey, false)
        grid = settings.getBoolean("grid_enabled", !isHuaweiOrHonor())
        autoPurchase = settings.getBoolean("auto_purchase", true)
        autoDungeon = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("auto_dungeon", true)
        autoNetworkDefense = false
        legacyCapture = settings.getBoolean("legacy_capture", true)
        summonTouchCorrection = settings.getBoolean("summon_touch_correction", false)
        preReleaseUpdates = settings.getBoolean("pre_release_updates", false)
        supporterLicense = SupporterLicenseManager.load(this)
        autoNetworkDefense = supporterLicense != null && settings.getBoolean("auto_network_defense", false)
        autoFeed = settings.getBoolean("auto_feed", false)
        dwsNeverLeft = supporterLicense != null && settings.getBoolean("dws_never_left", false)
        dwsForceForwardAttack = dwsNeverLeft
        dwsDashSpam = supporterLicense != null && settings.getBoolean("dws_dash_spam", false)
        dwsOnlyEnergy = supporterLicense != null && settings.getBoolean("dws_only_energy", false)
        dwsBetterCollect = true
        dwsBlindStageTap = supporterLicense != null && settings.getBoolean("dws_blind_stage_tap", false)
        if (autoNetworkDefense && autoFeed) {
            autoFeed = false
            settings.edit().putBoolean("auto_feed", false).apply()
        }
        AutomationState.overlayEnabled = grid
        AutomationState.autoPurchaseEnabled = autoPurchase
        AutomationState.autoDungeonEnabled = autoDungeon
        AutomationState.autoNetworkDefenseEnabled = autoNetworkDefense
        AutomationState.autoFeedEnabled = autoFeed
        AutomationState.dwsNavigationSettings = DwsNavigationSettings(allowLeft = !dwsNeverLeft, forceForwardAttack = dwsForceForwardAttack, dashSpamUntilZero = dwsDashSpam, collectOnlyEnergy = dwsOnlyEnergy, betterEnergyCollect = dwsBetterCollect, blindStageFailedTap = dwsBlindStageTap)
        AutomationState.forceLegacyCaptureMetrics = legacyCapture
        AutomationState.summonTouchCorrection = summonTouchCorrection
        DigiWorldAccessibilityService.instance?.setOverlayEnabled(grid)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) {
            ControlScreen(
                status, capture, auto, grid, autoPurchase, autoDungeon, autoNetworkDefense, autoFeed, dwsNeverLeft, dwsForceForwardAttack, dwsDashSpam, dwsOnlyEnergy, dwsBetterCollect, dwsBlindStageTap, legacyCapture, summonTouchCorrection, preReleaseUpdates, supporterLicense, access, overlay, updateStatus, updateVersion,
                onAccess = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                onOverlay = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                onGrid = { grid = !grid; AutomationState.overlayEnabled = grid; DigiWorldAccessibilityService.instance?.setOverlayEnabled(grid); settings.edit().putBoolean("grid_enabled", grid).apply() },
                onAutoPurchase = { enabled -> autoPurchase = enabled; AutomationState.autoPurchaseEnabled = enabled; getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("auto_purchase", enabled).apply() },
                onAutoDungeon = { enabled -> autoDungeon = enabled; AutomationState.autoDungeonEnabled = enabled; getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("auto_dungeon", enabled).apply() },
                onAutoNetworkDefense = { enabled ->
                    val allowed = enabled && supporterLicense != null
                    NetworkDefenseFrameAnalyzer.reset()
                    autoNetworkDefense = allowed
                    AutomationState.autoNetworkDefenseEnabled = allowed
                    if (allowed) {
                        StageFailedFrameAnalyzer.reset()
                        autoFeed = false
                        AutomationState.autoFeedEnabled = false
                        FeedFrameAnalyzer.reset()
                    }
                    settings.edit().putBoolean("auto_network_defense", allowed).putBoolean("auto_feed", autoFeed).apply()
                    if (enabled && !allowed) showLicenseDialog = true
                },
                onAutoFeed = { enabled ->
                    FeedFrameAnalyzer.reset()
                    autoFeed = enabled
                    AutomationState.autoFeedEnabled = enabled
                    if (enabled) {
                        autoNetworkDefense = false
                        AutomationState.autoNetworkDefenseEnabled = false
                        NetworkDefenseFrameAnalyzer.reset()
                        if (!settings.getBoolean("feed_delay_notice_seen", false)) {
                            showFeedDelayNotice = true
                            settings.edit().putBoolean("feed_delay_notice_seen", true).apply()
                        }
                    }
                    settings.edit().putBoolean("auto_feed", enabled).putBoolean("auto_network_defense", autoNetworkDefense).apply()
                },
                onDwsSettings = { neverLeft, _, dashSpam, onlyEnergy, betterCollect, blindStageTap ->
                    val allowed = supporterLicense != null
                    dwsNeverLeft = allowed && neverLeft
                    dwsForceForwardAttack = dwsNeverLeft
                    dwsDashSpam = allowed && dashSpam
                    dwsOnlyEnergy = allowed && onlyEnergy
                    dwsBetterCollect = true
                    dwsBlindStageTap = allowed && blindStageTap
                    AutomationState.dwsNavigationSettings = DwsNavigationSettings(
                        allowLeft = !dwsNeverLeft,
                        forceForwardAttack = dwsForceForwardAttack,
                        dashSpamUntilZero = dwsDashSpam,
                        collectOnlyEnergy = dwsOnlyEnergy,
                        betterEnergyCollect = dwsBetterCollect,
                        blindStageFailedTap = dwsBlindStageTap,
                    )
                    AutoMoveController.reset()
                    settings.edit()
                        .putBoolean("dws_never_left", dwsNeverLeft)
                        .putBoolean("dws_force_forward_attack", dwsForceForwardAttack)
                        .putBoolean("dws_dash_spam", dwsDashSpam)
                        .putBoolean("dws_only_energy", dwsOnlyEnergy)
                        .putBoolean("dws_better_collect", dwsBetterCollect)
                        .putBoolean("dws_blind_stage_tap", dwsBlindStageTap)
                        .apply()
                },
                onLegacyCapture = { enabled -> ScreenCaptureService.stop(this); capture = false; auto = false; status = UiStatus.STOPPED; legacyCapture = enabled; AutomationState.forceLegacyCaptureMetrics = enabled; settings.edit().putBoolean("legacy_capture", enabled).apply() },
                onSummonTouchCorrection = { enabled -> ScreenCaptureService.stop(this); capture = false; auto = false; status = UiStatus.STOPPED; summonTouchCorrection = enabled; AutomationState.summonTouchCorrection = enabled; settings.edit().putBoolean("summon_touch_correction", enabled).apply() },
                onPreReleaseUpdates = { enabled -> preReleaseUpdates = enabled; settings.edit().putBoolean("pre_release_updates", enabled).apply(); checkForUpdates() },
                onStart = ::requestAutomationStart,
                onStop = { ScreenCaptureService.stop(this); capture = false; auto = false; status = UiStatus.STOPPED },
                onLanguage = ::setLanguage,
                onCheckUpdate = ::checkForUpdates,
                onOpenUpdate = { if (updateUrl.isNotBlank()) openUrl(updateUrl) },
                onDonate = { openUrl(getString(R.string.donate_url)) },
                onLicense = { showLicenseDialog = true },
                onRepo = { openUrl(getString(R.string.footer_repo_url)) },
                onContact = { },
                onCommunity = { openUrl(getString(R.string.discord_url)) },
            )
            if (showFeedDelayNotice) FeedDelayNoticeDialog(onClose = { showFeedDelayNotice = false })
            if (showReleaseNotes) ReleaseNotesDialog(onClose = {
                showReleaseNotes = false
                settings.edit().putBoolean(releaseNotesKey, true).apply()
                if (!settings.getBoolean(communityIntroKey, false)) showCommunityIntro = true
            })
            if (showCommunityIntro) CommunityIntroDialog(
                onJoin = {
                    showCommunityIntro = false
                    settings.edit().putBoolean(communityIntroKey, true).apply()
                    openUrl(getString(R.string.discord_url))
                },
                onClose = {
                    showCommunityIntro = false
                    settings.edit().putBoolean(communityIntroKey, true).apply()
                },
            )
            if (showUpdateNotice) UpdateAvailableDialog(
                version = updateVersion,
                onUpdate = { showUpdateNotice = false; if (updateUrl.isNotBlank()) openUrl(updateUrl) },
                onLater = { showUpdateNotice = false },
            )
            if (showLicenseDialog) SupporterLicenseDialog(
                currentLicense = supporterLicense,
                onActivate = { code ->
                    val license = SupporterLicenseManager.activate(this, code)
                    if (license != null) supporterLicense = license
                    license != null
                },
                onRemove = {
                    SupporterLicenseManager.remove(this)
                    supporterLicense = null
                    dwsNeverLeft = false; dwsForceForwardAttack = false; dwsDashSpam = false; dwsOnlyEnergy = false; dwsBetterCollect = true; dwsBlindStageTap = false; AutomationState.dwsNavigationSettings = DwsNavigationSettings()
                    settings.edit().putBoolean("dws_never_left", false).putBoolean("dws_force_forward_attack", false).putBoolean("dws_dash_spam", false).putBoolean("dws_only_energy", false).putBoolean("dws_better_collect", false).putBoolean("dws_blind_stage_tap", false).apply()
                },
                onDonate = { openUrl(getString(R.string.supporter_purchase_url)) },
                onClose = { showLicenseDialog = false },
            )
            if (showSupportPrompt) SupportPromptDialog(
                onContinue = { showSupportPrompt = false; beginAutomation() },
                onDonate = { showSupportPrompt = false; openUrl(getString(R.string.donate_url)) },
            )
        } } }
        checkForUpdates()
    }

    private fun requestAutomationStart() {
        if (!access || auto) return
        if (!capture) {
            startAfterCapture = true
            consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
            return
        }
        continueAutomationStart()
    }

    private fun continueAutomationStart() {
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val starts = preferences.getInt("automation_start_count", 0) + 1
        preferences.edit().putInt("automation_start_count", starts).apply()
        if (supporterLicense == null && starts % 5 == 0) showSupportPrompt = true else beginAutomation()
    }

    private fun beginAutomation() {
        ScreenCaptureService.setAutomation(this, true)
        auto = true
        status = UiStatus.AUTOMATIC
        bringGameToForeground()
    }
    private fun syncSessionUi(automationEnabled: Boolean = AutomationState.enabled) {
        val session = CaptureSessionState.snapshot(automationEnabled)
        capture = session.captureActive
        auto = session.automationActive
        status = when {
            session.automationActive -> UiStatus.AUTOMATIC
            session.captureActive -> UiStatus.CAPTURING
            else -> UiStatus.STOPPED
        }
    }

    private fun checkForUpdates() {
        updateStatus = UpdateStatus.CHECKING
        UpdateChecker.check(BuildConfig.VERSION_NAME, preReleaseUpdates) { result -> runOnUiThread {
            when (result) {
                UpdateResult.Current -> updateStatus = UpdateStatus.CURRENT
                UpdateResult.Failed -> updateStatus = UpdateStatus.FAILED
                is UpdateResult.Available -> {
                    updateStatus = UpdateStatus.AVAILABLE; updateVersion = result.version; updateUrl = result.url
                    val noticeKey = "update_notice_${result.version}"
                    val preferences = getSharedPreferences("settings", MODE_PRIVATE)
                    if (!preferences.getBoolean(noticeKey, false)) {
                        preferences.edit().putBoolean(noticeKey, true).apply()
                        showUpdateNotice = true
                    }
                }
            }
        } }
    }

    private fun bringGameToForeground() {
        packageManager.getLaunchIntentForPackage(GAME_PACKAGE)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
    }

    private fun openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    private fun setLanguage(tag: String) {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putString("language", tag).apply()
        if (Build.VERSION.SDK_INT >= 33) getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        else { val config = resources.configuration; config.setLocale(Locale.forLanguageTag(tag)); resources.updateConfiguration(config, resources.displayMetrics); recreate() }
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) ScreenCaptureService.stop(this)
        super.onDestroy()
    }

    private fun refreshPermissions() {
        val expected = DigiWorldAccessibilityService::class.java.name
        val managerEnabled = (getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                val info = it.resolveInfo.serviceInfo
                info.packageName == packageName && (info.name == expected || info.name.endsWith(".DigiWorldAccessibilityService"))
            }
        val secureEnabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.split(':')?.any { it.contains(packageName, true) && it.contains("DigiWorldAccessibilityService", true) } == true
        access = DigiWorldAccessibilityService.instance != null || managerEnabled || secureEnabled
        overlay = Settings.canDrawOverlays(this)
        syncSessionUi()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        window.decorView.postDelayed({ if (!isFinishing) refreshPermissions() }, 500L)
    }
}

@Composable private fun ControlScreen(status: UiStatus, capture: Boolean, auto: Boolean, grid: Boolean, autoPurchase: Boolean, autoDungeon: Boolean, autoNetworkDefense: Boolean, autoFeed: Boolean, dwsNeverLeft: Boolean, dwsForceForwardAttack: Boolean, dwsDashSpam: Boolean, dwsOnlyEnergy: Boolean, dwsBetterCollect: Boolean, dwsBlindStageTap: Boolean, legacyCapture: Boolean, summonTouchCorrection: Boolean, preReleaseUpdates: Boolean, supporterLicense: SupporterLicense?, access: Boolean, overlay: Boolean, update: UpdateStatus, updateVersion: String, onAccess: () -> Unit, onOverlay: () -> Unit, onGrid: () -> Unit, onAutoPurchase: (Boolean) -> Unit, onAutoDungeon: (Boolean) -> Unit, onAutoNetworkDefense: (Boolean) -> Unit, onAutoFeed: (Boolean) -> Unit, onDwsSettings: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit, onLegacyCapture: (Boolean) -> Unit, onSummonTouchCorrection: (Boolean) -> Unit, onPreReleaseUpdates: (Boolean) -> Unit, onStart: () -> Unit, onStop: () -> Unit, onLanguage: (String) -> Unit, onCheckUpdate: () -> Unit, onOpenUpdate: () -> Unit, onDonate: () -> Unit, onLicense: () -> Unit, onRepo: () -> Unit, onContact: () -> Unit, onCommunity: () -> Unit) {
    var showAccessHelp by remember { mutableStateOf(false) }
    var featureHelp by remember { mutableStateOf<Int?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var showExperimental by remember { mutableStateOf(false) }
    var showDwsSettings by remember { mutableStateOf(false) }
    val statusText = when (status) { UiStatus.READY -> R.string.status_ready; UiStatus.CAPTURING -> R.string.status_capture; UiStatus.AUTOMATIC -> R.string.status_auto; UiStatus.CAPTURE_DENIED -> R.string.status_capture_denied; UiStatus.STOPPED -> R.string.status_stopped }
    if (showAccessHelp) AlertDialog(onDismissRequest = { showAccessHelp = false }, title = { Text(stringResource(R.string.accessibility_help_title)) }, text = { Text(stringResource(R.string.accessibility_help_body)) }, confirmButton = { TextButton(onClick = { showAccessHelp = false }) { Text(stringResource(R.string.close)) } })
    featureHelp?.let { FeatureHelpDialog(it, onClose = { featureHelp = null }) }
    if (showDwsSettings) DwsSettingsDialog(dwsNeverLeft, dwsForceForwardAttack, dwsDashSpam, dwsOnlyEnergy, dwsBetterCollect, dwsBlindStageTap, supporterLicense != null, onDwsSettings, onUnlock = onLicense, onClose = { showDwsSettings = false })
    if (showExperimental) ExperimentalSettingsDialog(legacyCapture, summonTouchCorrection, preReleaseUpdates, onLegacyCapture, onSummonTouchCorrection, onPreReleaseUpdates, onClose = { showExperimental = false })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 29.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF075E73), Color(0xFF168A75), Color(0xFF514A93))), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_title), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("v${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelSmall)
            }
            Text(if (auto) "●" else "○", color = if (auto) Color(0xFF7CFFB2) else Color.White.copy(alpha = .75f), modifier = Modifier.align(Alignment.CenterEnd))
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onGrid, enabled = overlay, modifier = Modifier.weight(1f)) { Text(stringResource(if (grid) R.string.grid_hide else R.string.grid_show)) }
            Button(onClick = onStart, enabled = access && !auto, modifier = Modifier.weight(1f)) { Text(stringResource(if (auto) R.string.bot_running else if (capture) R.string.auto_start else R.string.auto_start_with_capture)) }
        }
        FeatureInfoRow(R.string.digiworld_help_title, onAdvanced = { showDwsSettings = true }, onHelp = { featureHelp = 2 })
        FeatureSwitch(R.string.auto_purchase, autoPurchase, onAutoPurchase, onHelp = { featureHelp = 0 })
        FeatureSwitch(R.string.auto_dungeon, autoDungeon, onAutoDungeon, onHelp = { featureHelp = 1 })
        FeatureSwitch(R.string.auto_feed, autoFeed, onAutoFeed, onHelp = { featureHelp = 4 })
        FeatureSwitch(R.string.auto_network_defense, autoNetworkDefense, onAutoNetworkDefense, onHelp = { featureHelp = 3 }, enabled = supporterLicense != null, supporterStyle = true)
        ComingSoonFeatureRow(onHelp = { featureHelp = 5 })
        Button(onClick = onStop, enabled = capture || auto, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.auto_stop)) }

        Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.labelSmall)
        if (supporterLicense == null) {
            Button(onClick = onDonate, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168A45))) { Text(stringResource(R.string.donate), fontSize = 11.sp, maxLines = 1) }
        }
        OutlinedButton(onClick = onLicense, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (supporterLicense != null) Color(0xFFE3F5E9) else Color.Transparent, contentColor = if (supporterLicense != null) Color(0xFF176B3A) else MaterialTheme.colorScheme.primary)) {
            Text(stringResource(if (supporterLicense == null) R.string.supporter_license_import else R.string.supporter_license_active), maxLines = 1, fontWeight = if (supporterLicense != null) FontWeight.SemiBold else FontWeight.Normal)
        }
        OutlinedButton(onClick = { showExperimental = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.experimental_settings)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = onRepo, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp)) { Text(stringResource(R.string.source_code_short), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            OutlinedButton(onClick = { showContactDialog = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp)) { Text(stringResource(R.string.contact_me), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            OutlinedButton(onClick = onCommunity, modifier = Modifier.weight(1.25f), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp)) {
                Image(painter = painterResource(R.drawable.discord_logo), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.join_community), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onLanguage("de") }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("🇩🇪 DE") }
            TextButton(onClick = { onLanguage("en") }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("🇬🇧 EN") }
        }
        if (showContactDialog) {
            AlertDialog(
                onDismissRequest = { showContactDialog = false },
                title = { Text(stringResource(R.string.contact_title)) },
                text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.contact_body))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("support@robinthor.de", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                    }
                } },
                confirmButton = { Button(onClick = { clipboard.setText(AnnotatedString("support@robinthor.de")); showContactDialog = false }) { Text(stringResource(R.string.contact_copy)) } },
                dismissButton = { TextButton(onClick = { showContactDialog = false }) { Text(stringResource(R.string.close)) } }
            )
        }
    }
}

@Composable private fun DwsSettingsDialog(
    neverLeft: Boolean,
    forceForwardAttack: Boolean,
    dashSpam: Boolean,
    onlyEnergy: Boolean,
    betterCollect: Boolean,
    blindStageTap: Boolean,
    supporterUnlocked: Boolean,
    onSettings: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUnlock: () -> Unit,
    onClose: () -> Unit,
) {
    var helpKind by remember { mutableStateOf<Int?>(null) }
    helpKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { helpKind = null },
            title = { Text(stringResource(when (kind) { 0 -> R.string.dws_force_attack; 1 -> R.string.dws_dash_spam; 2 -> R.string.dws_only_energy; 3 -> R.string.dws_better_collect; else -> R.string.dws_blind_stage_tap })) },
            text = { Text(stringResource(when (kind) { 0 -> R.string.dws_force_attack_hint; 1 -> R.string.dws_dash_spam_hint; 2 -> R.string.dws_only_energy_hint; 3 -> R.string.dws_better_collect_hint; else -> R.string.dws_blind_stage_tap_hint })) },
            confirmButton = { TextButton(onClick = { helpKind = null }) { Text(stringResource(R.string.close)) } },
        )
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.dws_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dws_settings_body), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_force_attack), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 0 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = neverLeft && forceForwardAttack, enabled = supporterUnlocked, onCheckedChange = { onSettings(it, it, dashSpam, onlyEnergy, betterCollect, blindStageTap) })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_dash_spam), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 1 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = dashSpam, enabled = supporterUnlocked, onCheckedChange = { onSettings(neverLeft, neverLeft, it, onlyEnergy, betterCollect, blindStageTap) })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_only_energy), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 2 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = onlyEnergy, enabled = supporterUnlocked, onCheckedChange = { onSettings(neverLeft, neverLeft, dashSpam, it, betterCollect, blindStageTap) })
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_blind_stage_tap), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 4 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = blindStageTap, enabled = supporterUnlocked, onCheckedChange = { onSettings(neverLeft, neverLeft, dashSpam, onlyEnergy, betterCollect, it) })
                }
                if (!supporterUnlocked) {
                    OutlinedButton(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.dws_unlock_options)) }
                }

            }
        },
        confirmButton = { Button(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
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

@Composable private fun FeatureInfoRow(label: Int, onAdvanced: (() -> Unit)? = null, onHelp: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        if (onAdvanced != null) {
            TextButton(
                onClick = onAdvanced,
                modifier = Modifier.background(Color(0xFFDDF3E5), RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            ) { Text(stringResource(R.string.dws_settings_button), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, maxLines = 1) }
        }
        IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun ComingSoonFeatureRow(onHelp: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.partner_rotation_preview), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                Text(stringResource(R.string.coming_soon_supporter), style = MaterialTheme.typography.labelSmall, color = Color(0xFF3F7F59).copy(alpha = .78f))
            }
            IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun FeatureSwitch(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit, onHelp: () -> Unit, enabled: Boolean = true, supporterStyle: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).then(if (supporterStyle) Modifier.background(Color(0xFFE3F5E9), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp) else Modifier), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", fontWeight = FontWeight.Bold) }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable private fun FeatureHelpDialog(kind: Int, onClose: () -> Unit) {
    val title = when (kind) { 0 -> R.string.summon_help_title; 1 -> R.string.dungeon_help_title; 2 -> R.string.digiworld_help_title; 3 -> R.string.network_help_title; 5 -> R.string.partner_rotation_help_title; else -> R.string.feed_help_title }
    val body = when (kind) { 0 -> R.string.summon_help_body; 1 -> R.string.dungeon_help_body; 2 -> R.string.digiworld_help_body; 3 -> R.string.network_help_body; 5 -> R.string.partner_rotation_help_body; else -> R.string.feed_help_body }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(body))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

@Composable private fun SupportPromptDialog(onContinue: () -> Unit, onDonate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text(stringResource(R.string.support_prompt_title)) },
        text = { Text(stringResource(R.string.support_prompt_body)) },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.support_prompt_continue)) } },
        dismissButton = { TextButton(onClick = onDonate) { Text(stringResource(R.string.support_prompt_donate)) } },
    )
}
@Composable private fun FeedDelayNoticeDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.feed_delay_notice_title)) },
        text = { Text(stringResource(R.string.feed_delay_notice_body)) },
        confirmButton = { Button(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}
@Composable private fun ReleaseNotesDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.release_notes_title)) },
        text = { Text(stringResource(R.string.release_notes_body)) },
        confirmButton = { Button(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

@Composable private fun CommunityIntroDialog(onJoin: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        icon = { Image(painterResource(R.drawable.discord_logo), contentDescription = null, Modifier.size(64.dp)) },
        title = { Text(stringResource(R.string.community_intro_title)) },
        text = { Text(stringResource(R.string.community_intro_body)) },
        confirmButton = { Button(onClick = onJoin) { Text(stringResource(R.string.community_intro_join)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}
@Composable private fun UpdateAvailableDialog(version: String, onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_notice_title)) },
        text = { Text(stringResource(R.string.update_notice_body, version)) },
        confirmButton = { Button(onClick = onUpdate) { Text(stringResource(R.string.update_notice_install)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.update_notice_later)) } },
    )
}
@Composable private fun PaymentContactDialog(onContinue: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.payment_contact_title)) },
        text = { Text(stringResource(R.string.payment_contact_body)) },
        confirmButton = { Button(onClick = onContinue) { Text(stringResource(R.string.payment_continue)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

@Composable private fun SupporterLicenseDialog(
    currentLicense: SupporterLicense?,
    onActivate: (String) -> Boolean,
    onRemove: () -> Unit,
    onDonate: () -> Unit,
    onClose: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var showPaymentInfo by remember { mutableStateOf(false) }
    if (showPaymentInfo) {
        PaymentContactDialog(onContinue = { showPaymentInfo = false; onDonate() }, onClose = { showPaymentInfo = false })
        return
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.supporter_license_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentLicense != null) {
                    Text(stringResource(R.string.supporter_license_enabled))
                    Text(currentLicense.licenseId, fontWeight = FontWeight.Bold)
                } else {
                    Text(stringResource(R.string.supporter_license_body))
                    Button(onClick = { showPaymentInfo = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168A45))) {
                        Text(stringResource(R.string.supporter_license_paypal))
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; invalid = false },
                        label = { Text(stringResource(R.string.supporter_license_code)) },
                        isError = invalid,
                        supportingText = if (invalid) ({ Text(stringResource(R.string.supporter_license_invalid)) }) else null,
                        minLines = 3,
                    )
                }
            }
        },
        confirmButton = {
            if (currentLicense == null) Button(onClick = {
                if (onActivate(code)) onClose() else invalid = true
            }, enabled = code.isNotBlank()) { Text(stringResource(R.string.supporter_license_activate)) }
            else TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
        dismissButton = {
            if (currentLicense != null) TextButton(onClick = onRemove) { Text(stringResource(R.string.supporter_license_remove)) }
            else TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
    )
}
@Composable private fun ExperimentalSettingsDialog(
    legacyCapture: Boolean,
    summonTouchCorrection: Boolean,
    preReleaseUpdates: Boolean,
    onLegacyCapture: (Boolean) -> Unit,
    onSummonTouchCorrection: (Boolean) -> Unit,
    onPreReleaseUpdates: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.experimental_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.legacy_capture))
                        Text(stringResource(R.string.legacy_capture_hint), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = legacyCapture, onCheckedChange = onLegacyCapture)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.summon_touch_correction))
                        Text(stringResource(R.string.summon_touch_correction_hint), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = summonTouchCorrection, onCheckedChange = onSummonTouchCorrection)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pre_release_updates))
                        Text(stringResource(R.string.pre_release_updates_hint), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = preReleaseUpdates, onCheckedChange = onPreReleaseUpdates)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

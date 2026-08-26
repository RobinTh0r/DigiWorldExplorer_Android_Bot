package de.robinthor.digiworldexplorer

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.CaptureSessionState
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer
import de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer
import de.robinthor.digiworldexplorer.dungeon.DungeonFrameAnalyzer
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
    private var dwsBlindStageTap by mutableStateOf(true)
    private var showSupportPrompt by mutableStateOf(false)
    private var legacyCapture by mutableStateOf(false)
    private var summonTouchCorrection by mutableStateOf(false)
    private var preReleaseUpdates by mutableStateOf(false)
    private var darkMode by mutableStateOf(false)
    private var quickOverlayEnabled by mutableStateOf(false)
    private var supporterLicense by mutableStateOf<SupporterLicense?>(null)
    private var showLicenseDialog by mutableStateOf(false)
    private var showReleaseNotes by mutableStateOf(false)
    private var showCommunityIntro by mutableStateOf(false)
    private var showUpdateNotice by mutableStateOf(false)
    private var showFeedDelayNotice by mutableStateOf(false)
    private var access by mutableStateOf(false)
    private var overlay by mutableStateOf(false)
    private var batteryExempt by mutableStateOf(false)
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
        darkMode = settings.getBoolean("dark_mode", false)
        supporterLicense = SupporterLicenseManager.load(this)
        quickOverlayEnabled = supporterLicense != null && settings.getBoolean("quick_overlay_enabled", false)
        autoNetworkDefense = supporterLicense != null && settings.getBoolean("auto_network_defense", false)
        autoFeed = settings.getBoolean("auto_feed", false)
        dwsNeverLeft = supporterLicense != null && settings.getBoolean("dws_never_left", false)
        dwsForceForwardAttack = dwsNeverLeft
        dwsDashSpam = supporterLicense != null && settings.getBoolean("dws_dash_spam", false)
        dwsOnlyEnergy = supporterLicense != null && settings.getBoolean("dws_only_energy", false)
        dwsBetterCollect = true
        dwsBlindStageTap = true
        settings.edit().putBoolean("dws_blind_stage_tap", true).apply()
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
        setContent {
            val colors = if (darkMode) darkColorScheme(
                background = Color(0xFF101216),
                onBackground = Color(0xFFF4F5F6),
                surface = Color(0xFF1B1E24),
                onSurface = Color(0xFFF4F5F6),
                surfaceVariant = Color(0xFF22262D),
                onSurfaceVariant = Color(0xFFAEB5BE),
                primary = Color(0xFF32B8B4),
                onPrimary = Color(0xFF071817),
                primaryContainer = Color(0xFF183C3C),
                onPrimaryContainer = Color(0xFFBDF4F1),
                secondary = Color(0xFFAEB5BE),
                onSecondary = Color(0xFF101216),
                secondaryContainer = Color(0xFF22262D),
                onSecondaryContainer = Color(0xFFF4F5F6),
                tertiary = Color(0xFF39B76A),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFF193B29),
                onTertiaryContainer = Color(0xFFBDF3CF),
                error = Color(0xFFE65C61),
                onError = Color.White,
                outline = Color(0xFF343941),
            ) else lightColorScheme(
                background = Color(0xFFF6F7F9),
                onBackground = Color(0xFF20242A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF20242A),
                surfaceVariant = Color(0xFFEFF1F4),
                onSurfaceVariant = Color(0xFF68717C),
                primary = Color(0xFF147F82),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD7F0F0),
                onPrimaryContainer = Color(0xFF0B5759),
                secondary = Color(0xFF68717C),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFEFF1F4),
                onSecondaryContainer = Color(0xFF20242A),
                tertiary = Color(0xFF248A4D),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFDDF4E6),
                onTertiaryContainer = Color(0xFF155D34),
                error = Color(0xFFC94349),
                onError = Color.White,
                outline = Color(0xFFD8DDE3),
            )
            MaterialTheme(colorScheme = colors) { Surface(Modifier.fillMaxSize(), color = colors.background) {
            ControlScreen(
                status, capture, auto, grid, autoPurchase, autoDungeon, autoNetworkDefense, autoFeed, dwsNeverLeft, dwsForceForwardAttack, dwsDashSpam, dwsOnlyEnergy, dwsBetterCollect, dwsBlindStageTap, legacyCapture, summonTouchCorrection, preReleaseUpdates, darkMode, quickOverlayEnabled, supporterLicense, access, overlay, batteryExempt, updateStatus, updateVersion,
                onAccess = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                onOverlay = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                onBattery = ::requestBatteryOptimizationExemption,
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
                    }
                    settings.edit().putBoolean("auto_network_defense", allowed).apply()
                    if (enabled && !allowed) showLicenseDialog = true
                },
                onAutoFeed = { enabled ->
                    FeedFrameAnalyzer.reset()
                    autoFeed = enabled
                    AutomationState.autoFeedEnabled = enabled
                    if (enabled) {
                        if (!settings.getBoolean("feed_delay_notice_seen", false)) {
                            showFeedDelayNotice = true
                            settings.edit().putBoolean("feed_delay_notice_seen", true).apply()
                        }
                    }
                    settings.edit().putBoolean("auto_feed", enabled).apply()
                },
                onDwsSettings = { neverLeft, _, dashSpam, onlyEnergy, betterCollect, _ ->
                    val allowed = supporterLicense != null
                    dwsNeverLeft = allowed && neverLeft
                    dwsForceForwardAttack = dwsNeverLeft
                    dwsDashSpam = allowed && dashSpam
                    dwsOnlyEnergy = allowed && onlyEnergy
                    dwsBetterCollect = true
                    dwsBlindStageTap = true
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
                onDarkMode = { enabled -> darkMode = enabled; settings.edit().putBoolean("dark_mode", enabled).apply() },
                onQuickOverlay = { enabled ->
                    val allowed = supporterLicense != null
                    quickOverlayEnabled = enabled && allowed
                    settings.edit().putBoolean("quick_overlay_enabled", quickOverlayEnabled).apply()
                    DigiWorldAccessibilityService.instance?.setQuickControlsEnabled(quickOverlayEnabled)
                    if (enabled && !allowed) showLicenseDialog = true
                    else if (enabled && DigiWorldAccessibilityService.instance == null) {
                        Toast.makeText(this, getString(R.string.quick_overlay_reconnect_accessibility), Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                onStart = ::requestAutomationStart,
                onReturnToGame = ::bringGameToForeground,
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
                    dwsNeverLeft = false; dwsForceForwardAttack = false; dwsDashSpam = false; dwsOnlyEnergy = false; dwsBetterCollect = true; dwsBlindStageTap = true; AutomationState.dwsNavigationSettings = DwsNavigationSettings(blindStageFailedTap = true)
                    settings.edit().putBoolean("dws_never_left", false).putBoolean("dws_force_forward_attack", false).putBoolean("dws_dash_spam", false).putBoolean("dws_only_energy", false).putBoolean("dws_better_collect", true).putBoolean("dws_blind_stage_tap", true).apply()
                },
                onDonate = { openUrl(getString(R.string.supporter_purchase_url)) },
                onRequestDiscord = { openUrl(getString(R.string.discord_url)) },
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
        // A feature analyzer may stop its current session after a safety timeout. Re-apply the
        // switches shown in the UI whenever the user starts automation again so a stale runtime
        // value cannot leave an enabled VS/Tower switch reporting "Loop off".
        AutomationState.overlayEnabled = grid
        AutomationState.autoPurchaseEnabled = autoPurchase
        AutomationState.autoDungeonEnabled = autoDungeon
        AutomationState.autoNetworkDefenseEnabled = autoNetworkDefense
        AutomationState.autoFeedEnabled = autoFeed
        DungeonFrameAnalyzer.reset()
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

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val request = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        runCatching { startActivity(request) }.getOrElse {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
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
        // Android/OEMs may leave the secure toggle enabled even after the service process crashed.
        // Only a live service can execute taps or create the quick-control overlay.
        access = DigiWorldAccessibilityService.instance != null
        overlay = Settings.canDrawOverlays(this)
        batteryExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        } else true
        syncSessionUi()
    }

    override fun onResume() {
        super.onResume()
        val settings = getSharedPreferences("settings", MODE_PRIVATE)
        autoPurchase = settings.getBoolean("auto_purchase", true)
        autoDungeon = settings.getBoolean("auto_dungeon", true)
        autoFeed = settings.getBoolean("auto_feed", false)
        supporterLicense = SupporterLicenseManager.load(this)
        autoNetworkDefense = supporterLicense != null && settings.getBoolean("auto_network_defense", false)
        quickOverlayEnabled = supporterLicense != null && settings.getBoolean("quick_overlay_enabled", false)
        refreshPermissions()
        window.decorView.postDelayed({ if (!isFinishing) refreshPermissions() }, 500L)
    }
}

@Composable private fun ControlScreen(status: UiStatus, capture: Boolean, auto: Boolean, grid: Boolean, autoPurchase: Boolean, autoDungeon: Boolean, autoNetworkDefense: Boolean, autoFeed: Boolean, dwsNeverLeft: Boolean, dwsForceForwardAttack: Boolean, dwsDashSpam: Boolean, dwsOnlyEnergy: Boolean, dwsBetterCollect: Boolean, dwsBlindStageTap: Boolean, legacyCapture: Boolean, summonTouchCorrection: Boolean, preReleaseUpdates: Boolean, darkMode: Boolean, quickOverlayEnabled: Boolean, supporterLicense: SupporterLicense?, access: Boolean, overlay: Boolean, batteryExempt: Boolean, update: UpdateStatus, updateVersion: String, onAccess: () -> Unit, onOverlay: () -> Unit, onBattery: () -> Unit, onGrid: () -> Unit, onAutoPurchase: (Boolean) -> Unit, onAutoDungeon: (Boolean) -> Unit, onAutoNetworkDefense: (Boolean) -> Unit, onAutoFeed: (Boolean) -> Unit, onDwsSettings: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit, onLegacyCapture: (Boolean) -> Unit, onSummonTouchCorrection: (Boolean) -> Unit, onPreReleaseUpdates: (Boolean) -> Unit, onDarkMode: (Boolean) -> Unit, onQuickOverlay: (Boolean) -> Unit, onStart: () -> Unit, onReturnToGame: () -> Unit, onStop: () -> Unit, onLanguage: (String) -> Unit, onCheckUpdate: () -> Unit, onOpenUpdate: () -> Unit, onDonate: () -> Unit, onLicense: () -> Unit, onRepo: () -> Unit, onContact: () -> Unit, onCommunity: () -> Unit) {
    var showAccessHelp by remember { mutableStateOf(false) }
    var setupExpanded by remember { mutableStateOf(!(access && overlay && batteryExempt)) }
    var featureHelp by remember { mutableStateOf<Int?>(null) }
    var showContactDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var showExperimental by remember { mutableStateOf(false) }
    var showDwsSettings by remember { mutableStateOf(false) }
    val statusText = when (status) { UiStatus.READY -> R.string.status_ready; UiStatus.CAPTURING -> R.string.status_capture; UiStatus.AUTOMATIC -> R.string.status_auto; UiStatus.CAPTURE_DENIED -> R.string.status_capture_denied; UiStatus.STOPPED -> R.string.status_stopped }
if (showAccessHelp) TroubleshootingAssistantDialog(
        access = access,
        overlay = overlay,
        batteryExempt = batteryExempt,
        grid = grid,
        quickOverlayEnabled = quickOverlayEnabled,
        legacyCapture = legacyCapture,
        onAccess = onAccess,
        onOverlay = onOverlay,
        onBattery = onBattery,
        onGrid = onGrid,
        onQuickOverlay = onQuickOverlay,
        onLegacyCapture = onLegacyCapture,
        onRestart = onStart,
        onClose = { showAccessHelp = false },
    )
    featureHelp?.let { FeatureHelpDialog(it, onClose = { featureHelp = null }) }
    if (showDwsSettings) DwsSettingsDialog(dwsNeverLeft, dwsForceForwardAttack, dwsDashSpam, dwsOnlyEnergy, dwsBetterCollect, dwsBlindStageTap, supporterLicense != null, onDwsSettings, onUnlock = onLicense, onClose = { showDwsSettings = false })
    if (showExperimental) ExperimentalSettingsDialog(legacyCapture, summonTouchCorrection, preReleaseUpdates, onLegacyCapture, onSummonTouchCorrection, onPreReleaseUpdates, onClose = { showExperimental = false })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 29.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_title), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Text(if (auto) "●" else "○", color = if (auto) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterEnd))
            OutlinedButton(
                onClick = { onDarkMode(!darkMode) },
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(if (darkMode) "☀" else "☾", fontSize = 18.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .75f)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactStatusCard(statusText, Modifier.weight(1.15f))
            CompactUpdateCard(update, updateVersion, onCheckUpdate, onOpenUpdate, Modifier.weight(.85f))
        }
        val setupComplete = access && overlay && batteryExempt
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    val setupCount = listOf(access, overlay, batteryExempt).count { it }
                    Text(
                        if (setupComplete) "✓ 3/3" else "$setupCount/3",
                        color = if (setupComplete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { showAccessHelp = true }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(stringResource(R.string.troubleshooting_button), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { setupExpanded = !setupExpanded }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(if (setupExpanded) "⌃" else "⌄") }
                }
                if (setupExpanded) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactPermissionButton(1, R.string.permission_accessibility, access, onAccess, Modifier.weight(1f))
                        CompactPermissionButton(2, R.string.permission_overlay, overlay, onOverlay, Modifier.weight(1f))
                    }
                    CompactPermissionButton(3, R.string.permission_battery, batteryExempt, onBattery, Modifier.fillMaxWidth())
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onQuickOverlay(!quickOverlayEnabled) },
                modifier = Modifier.weight(1.35f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = betaContainerColor(),
                    contentColor = betaContentColor(),
                ),
                border = BorderStroke(1.5.dp, betaBorderColor()),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp).alpha(if (quickOverlayEnabled) 1f else .24f),
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(if (quickOverlayEnabled) R.string.quick_overlay_disable else R.string.quick_overlay_enable), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(5.dp))
                BetaChip()
            }
            Button(
                onClick = if (auto) onReturnToGame else onStart,
                enabled = access,
                modifier = Modifier.weight(.9f),
                colors = ButtonDefaults.buttonColors(containerColor = if (auto) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
            ) {
                Text(
                    stringResource(if (auto) R.string.bot_return_game else R.string.auto_start),
                    fontSize = if (auto) 9.sp else 13.sp,
                    lineHeight = if (auto) 10.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        OutlinedButton(
            onClick = { showAccessHelp = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text("💬", fontSize = 19.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.troubleshooting_button), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.troubleshooting_button_hint), style = MaterialTheme.typography.labelSmall)
            }
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
        FeatureInfoRow(R.string.digiworld_help_title, grid = grid, gridEnabled = overlay, onGrid = onGrid, onAdvanced = { showDwsSettings = true }, onHelp = { featureHelp = 2 })
        FeatureSwitch(R.string.auto_purchase, autoPurchase, onAutoPurchase, onHelp = { featureHelp = 0 })
        FeatureSwitch(R.string.auto_dungeon, autoDungeon, onAutoDungeon, onHelp = { featureHelp = 1 })
        FeatureSwitch(R.string.auto_feed, autoFeed, onAutoFeed, onHelp = { featureHelp = 4 })
        FeatureSwitch(R.string.auto_network_defense, autoNetworkDefense, onAutoNetworkDefense, onHelp = { featureHelp = 3 }, enabled = supporterLicense != null, supporterStyle = true)
        ComingSoonFeatureRow(onHelp = { featureHelp = 5 })
        Button(onClick = onStop, enabled = capture || auto, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.auto_stop)) }

        Text(stringResource(R.string.safety_note), style = MaterialTheme.typography.labelSmall)
        if (supporterLicense == null) {
            OutlinedButton(
                onClick = onDonate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(stringResource(R.string.donate), fontSize = 11.sp, maxLines = 1) }
        }
        OutlinedButton(onClick = onLicense, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(containerColor = betaContainerColor(), contentColor = betaContentColor()), border = BorderStroke(1.25.dp, betaBorderColor())) {
            Text(stringResource(if (supporterLicense == null) R.string.supporter_license_import else R.string.supporter_license_active), maxLines = 1, fontWeight = if (supporterLicense != null) FontWeight.SemiBold else FontWeight.Normal)
            Spacer(Modifier.width(7.dp))
            BetaChip()
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

@Composable private fun TroubleshootingAssistantDialog(
    access: Boolean,
    overlay: Boolean,
    batteryExempt: Boolean,
    grid: Boolean,
    quickOverlayEnabled: Boolean,
    legacyCapture: Boolean,
    onAccess: () -> Unit,
    onOverlay: () -> Unit,
    onBattery: () -> Unit,
    onGrid: () -> Unit,
    onQuickOverlay: (Boolean) -> Unit,
    onLegacyCapture: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
) {
    var page by remember { mutableStateOf(0) }
    val title = when (page) {
        1 -> R.string.troubleshooting_setup_title
        2 -> R.string.troubleshooting_grid_title
        3 -> R.string.troubleshooting_samsung_title
        4 -> R.string.troubleshooting_start_title
        5 -> R.string.troubleshooting_network_title
        6 -> R.string.troubleshooting_summon_title
        7 -> R.string.troubleshooting_dungeon_title
        8 -> R.string.troubleshooting_feed_title
        9 -> R.string.troubleshooting_overlay_title
        else -> R.string.troubleshooting_title
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (page) {
                    0 -> {
                        Text(stringResource(R.string.troubleshooting_intro))
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_start_problem), emphasized = true) { page = 4 }
                        Text(stringResource(R.string.troubleshooting_category_setup), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_check_setup)) { page = 1 }
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_access_blocked)) { page = 3 }
                        Text(stringResource(R.string.troubleshooting_category_features), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_overlay_problem)) { page = 9 }
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_no_grid)) { page = 2 }
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_summon_problem)) { page = 6 }
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_dungeon_problem)) { page = 7 }
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_network_problem)) { page = 5 }
                        TroubleshootingChoice(stringResource(R.string.troubleshooting_feed_problem)) { page = 8 }
                    }
                    1 -> {
                        Text(stringResource(R.string.troubleshooting_setup_body))
                        CompactPermissionButton(1, R.string.permission_accessibility, access, onAccess, Modifier.fillMaxWidth())
                        CompactPermissionButton(2, R.string.permission_overlay, overlay, onOverlay, Modifier.fillMaxWidth())
                        CompactPermissionButton(3, R.string.permission_battery, batteryExempt, onBattery, Modifier.fillMaxWidth())
                    }
                    2 -> {
                        Text(stringResource(R.string.troubleshooting_grid_body))
                        Text(stringResource(R.string.troubleshooting_botamon), fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(if (grid) R.string.grid_hide else R.string.grid_show), modifier = Modifier.weight(1f))
                            Switch(checked = grid, onCheckedChange = { onGrid() }, colors = appSwitchColors())
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.legacy_capture))
                                Text(stringResource(R.string.troubleshooting_legacy_hint), style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(checked = legacyCapture, onCheckedChange = onLegacyCapture, colors = appSwitchColors())
                        }
                    }
                    4 -> {
                        Text(stringResource(R.string.troubleshooting_start_body))
                        CompactPermissionButton(1, R.string.permission_accessibility, access, onAccess, Modifier.fillMaxWidth())
                        CompactPermissionButton(2, R.string.permission_overlay, overlay, onOverlay, Modifier.fillMaxWidth())
                        CompactPermissionButton(3, R.string.permission_battery, batteryExempt, onBattery, Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.legacy_capture))
                                Text(stringResource(R.string.troubleshooting_legacy_default), style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(checked = legacyCapture, onCheckedChange = onLegacyCapture, colors = appSwitchColors())
                        }
                    }
                    5 -> Text(stringResource(R.string.troubleshooting_network_body))
                    6 -> Text(stringResource(R.string.troubleshooting_summon_body))
                    7 -> Text(stringResource(R.string.troubleshooting_dungeon_body))
                    8 -> Text(stringResource(R.string.troubleshooting_feed_body))
                    9 -> {
                        Text(stringResource(R.string.troubleshooting_overlay_body))
                        CompactPermissionButton(1, R.string.permission_accessibility, access, onAccess, Modifier.fillMaxWidth())
                        CompactPermissionButton(2, R.string.permission_overlay, overlay, onOverlay, Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.quick_overlay_enable))
                                Text(
                                    stringResource(if (quickOverlayEnabled) R.string.troubleshooting_overlay_enabled else R.string.troubleshooting_overlay_disabled),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Switch(
                                checked = quickOverlayEnabled,
                                onCheckedChange = onQuickOverlay,
                                colors = appSwitchColors(),
                            )
                        }
                        if (!access) {
                            Text(stringResource(R.string.troubleshooting_overlay_access_hint), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> Text(stringResource(R.string.troubleshooting_samsung_body))
                }
            }
        },
        confirmButton = {
            if (page == 0) TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
            else Button(onClick = { onClose(); onRestart() }) { Text(stringResource(R.string.troubleshooting_restart)) }
        },
        dismissButton = {
            if (page != 0) TextButton(onClick = { page = 0 }) { Text(stringResource(R.string.troubleshooting_back)) }
        },
    )
}

@Composable private fun TroubleshootingChoice(label: String, emphasized: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
            contentColor = if (emphasized) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text("›", style = MaterialTheme.typography.titleMedium)
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
                    Switch(checked = neverLeft && forceForwardAttack, enabled = supporterUnlocked, onCheckedChange = { onSettings(it, it, dashSpam, onlyEnergy, betterCollect, blindStageTap) }, colors = appSwitchColors())
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_dash_spam), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 1 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = dashSpam, enabled = supporterUnlocked, onCheckedChange = { onSettings(neverLeft, neverLeft, it, onlyEnergy, betterCollect, blindStageTap) }, colors = appSwitchColors())
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_only_energy), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 2 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = onlyEnergy, enabled = supporterUnlocked, onCheckedChange = { onSettings(neverLeft, neverLeft, dashSpam, it, betterCollect, blindStageTap) }, colors = appSwitchColors())
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dws_blind_stage_tap), modifier = Modifier.weight(1f))
                    IconButton(onClick = { helpKind = 4 }, modifier = Modifier.size(34.dp)) { Text("?", fontWeight = FontWeight.Bold) }
                    Switch(checked = true, enabled = false, onCheckedChange = null, colors = appSwitchColors())
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
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = if (status == UpdateStatus.AVAILABLE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 2)
            TextButton(onClick = if (status == UpdateStatus.AVAILABLE) onOpen else onCheck, contentPadding = PaddingValues(3.dp)) { Text(if (status == UpdateStatus.AVAILABLE) "↓" else "↻", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable private fun CompactPermissionButton(number: Int, label: Int, granted: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text("$number. ${stringResource(label)}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 2)
        Text(if (granted) "✓" else "○", fontWeight = FontWeight.Bold)
    }
}

@Composable private fun FeatureInfoRow(label: Int, grid: Boolean = true, gridEnabled: Boolean = true, onGrid: (() -> Unit)? = null, onAdvanced: (() -> Unit)? = null, onHelp: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(label).uppercase(),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            letterSpacing = 1.1.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (onAdvanced != null) {
            if (onGrid != null) {
                Surface(
                    onClick = onGrid,
                    enabled = gridEnabled,
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (grid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (grid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f)),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(if (grid) "▦" else "▧", fontSize = 20.sp) }
                }
                Spacer(Modifier.width(5.dp))
            }
            Surface(
                onClick = onAdvanced,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = betaContainerColor(),
                contentColor = betaContentColor(),
                border = BorderStroke(1.5.dp, betaBorderColor()),
            ) {
                Box(contentAlignment = Alignment.Center) { Text("⚙", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
            }
        }
        IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun ComingSoonFeatureRow(onHelp: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.partner_rotation_preview), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                Text(stringResource(R.string.coming_soon_supporter), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f))
            }
            IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun FeatureSwitch(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit, onHelp: () -> Unit, enabled: Boolean = true, supporterStyle: Boolean = false) {
    val rowColor = if (supporterStyle) betaContainerColor() else MaterialTheme.colorScheme.surface
    val contentColor = if (supporterStyle) betaContentColor() else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min = 42.dp).background(rowColor, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f), color = contentColor.copy(alpha = if (enabled) 1f else .72f))
        if (supporterStyle) {
            BetaChip()
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = onHelp, modifier = Modifier.size(36.dp)) { Text("?", color = contentColor, fontWeight = FontWeight.Bold) }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled, colors = appSwitchColors())
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

@Composable private fun BetaCodeRequestDialog(onDiscord: () -> Unit, onClose: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.beta_request_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.beta_request_body))
                Text(stringResource(R.string.beta_request_contact), fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { Button(onClick = onDiscord) { Text(stringResource(R.string.beta_request_discord)) } },
        dismissButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString("support@robinthor.de")) }) {
                Text(stringResource(R.string.beta_request_copy_email))
            }
        },
    )
}

@Composable private fun BetaChip() {
    Surface(
        color = betaGoldColor(),
        contentColor = if (MaterialTheme.colorScheme.background.luminance() < .45f) Color(0xFF201A08) else Color.White,
        shape = RoundedCornerShape(5.dp),
    ) {
        Text("BETA", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .6.sp)
    }
}

@Composable private fun appSwitchColors(): SwitchColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < .45f
    val disabled = if (dark) Color(0xFF626870) else Color(0xFFA6ADB5)
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = .88f),
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = .62f),
        disabledCheckedThumbColor = disabled,
        disabledCheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledCheckedBorderColor = MaterialTheme.colorScheme.outline,
        disabledUncheckedThumbColor = disabled,
        disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
        disabledUncheckedBorderColor = MaterialTheme.colorScheme.outline,
    )
}

@Composable private fun betaGoldColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < .45f) Color(0xFFE3AD32) else Color(0xFFB77B00)

@Composable private fun betaContainerColor(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < .45f
    return if (dark) Color(0xFF302711) else Color(0xFFFFF3CE)
}

@Composable private fun betaContentColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < .45f) Color(0xFFF2CA69) else Color(0xFF765000)

@Composable private fun betaBorderColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < .45f) Color(0xFFB98B25) else Color(0xFFD69B13)
@Composable private fun SupporterLicenseDialog(
    currentLicense: SupporterLicense?,
    onActivate: (String) -> Boolean,
    onRemove: () -> Unit,
    onDonate: () -> Unit,
    onRequestDiscord: () -> Unit,
    onClose: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var showPaymentInfo by remember { mutableStateOf(false) }
    var showBetaRequest by remember { mutableStateOf(false) }
    if (showPaymentInfo) {
        PaymentContactDialog(onContinue = { showPaymentInfo = false; onDonate() }, onClose = { showPaymentInfo = false })
        return
    }
    if (showBetaRequest) {
        BetaCodeRequestDialog(onDiscord = onRequestDiscord, onClose = { showBetaRequest = false })
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { showPaymentInfo = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168A45)),
                        ) {
                            Text(stringResource(R.string.supporter_license_paypal), fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showBetaRequest = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(stringResource(R.string.beta_request_button), fontSize = 11.sp, maxLines = 1)
                        }
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
                    Switch(checked = legacyCapture, onCheckedChange = onLegacyCapture, colors = appSwitchColors())
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.summon_touch_correction))
                        Text(stringResource(R.string.summon_touch_correction_hint), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = summonTouchCorrection, onCheckedChange = onSummonTouchCorrection, colors = appSwitchColors())
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pre_release_updates))
                        Text(stringResource(R.string.pre_release_updates_hint), style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = preReleaseUpdates, onCheckedChange = onPreReleaseUpdates, colors = appSwitchColors())
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

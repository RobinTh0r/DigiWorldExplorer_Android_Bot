package de.robinthor.digiworldexplorer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.CaptureFrameAnalyzer
import de.robinthor.digiworldexplorer.capture.CaptureSessionState
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.dungeon.DungeonFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer
import de.robinthor.digiworldexplorer.license.SupporterLicenseManager
import de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer
import de.robinthor.digiworldexplorer.purchase.RewardPurchaseFrameAnalyzer
import de.robinthor.digiworldexplorer.strategy.AutoMoveController
import de.robinthor.digiworldexplorer.strategy.AutomationState

/** Requests MediaProjection without bringing the full settings UI in front of the game. */
class CaptureConsentActivity : ComponentActivity() {
    companion object {
        private const val GAME_PACKAGE = "com.bandainamcoent.dgup_ww"
        const val EXTRA_START_MODE = "start_mode"
        const val START_COPILOT = "copilot"
        const val START_DUNGEON = "dungeon"
    }

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            applyRuntimeSettings()
            AutomationState.stop()
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            ScreenCaptureService.setAutomation(this, true)
            restoreOverlays()
            runRequestedMode()
            returnToGame()
        } else {
            Toast.makeText(this, getString(R.string.status_capture_denied), Toast.LENGTH_SHORT).show()
        }
        finishWithoutAnimation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CaptureSessionState.snapshot(AutomationState.enabled).captureActive) {
            applyRuntimeSettings()
            ScreenCaptureService.setAutomation(this, true)
            restoreOverlays()
            runRequestedMode()
            finishWithoutAnimation()
            return
        }
        val manager = getSystemService(MediaProjectionManager::class.java)
        consent.launch(manager.createScreenCaptureIntent())
    }

    private fun applyRuntimeSettings() {
        val settings = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val supporter = SupporterLicenseManager.load(this) != null
        AutomationState.mode = de.robinthor.digiworldexplorer.automation.AutomationMode.fromPreference(
            settings.getString("automation_mode", null))
        val dwsProfile = de.robinthor.digiworldexplorer.strategy.DwsNavigationProfile.fromPreferenceForAccess(
            settings.getString("dws_profile", null), supporter)
        AutomationState.overlayEnabled = settings.getBoolean("grid_enabled", true)
        AutomationState.autoPurchaseEnabled = settings.getBoolean("auto_purchase", false)
        AutomationState.autoDungeonEnabled = settings.getBoolean("auto_dungeon", false)
        AutomationState.autoNetworkDefenseEnabled = settings.getBoolean("auto_network_defense", false)
        AutomationState.autoFeedEnabled = settings.getBoolean("auto_feed", false)
        AutomationState.autoBondRotationEnabled = supporter && settings.getBoolean("auto_bond_rotation", true)
        AutomationState.copilotRewardsEnabled = supporter && settings.getBoolean("copilot_rewards", true)
        AutomationState.copilotDwsEnabled = supporter && settings.getBoolean("copilot_dws", true)
        AutomationState.autoFarmEnabled = supporter && settings.getBoolean("auto_farm_harvest", true)
        AutomationState.farmWateringEnabled = settings.getBoolean("farm_watering", true)
        AutomationState.adSkipPassEnabled = settings.getBoolean("ad_skip_pass", false)
        AutomationState.autoRunnerEnabled = false
        AutomationState.forceLegacyCaptureMetrics = settings.getBoolean("legacy_capture", true)
        AutomationState.summonTouchCorrection = settings.getBoolean("summon_touch_correction", false)
        AutomationState.dwsNavigationSettings = dwsProfile.settings()
        settings.edit().putBoolean("dws_blind_stage_tap", true).apply()
        RewardPurchaseFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.automation.GameEntryAnalyzer.reset()
        DungeonFrameAnalyzer.reset()
        NetworkDefenseFrameAnalyzer.reset()
        FeedFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.farm.FarmHarvestAnalyzer.reset()
        StageFailedFrameAnalyzer.reset()
        CaptureFrameAnalyzer.resetCalibration()
        AutoMoveController.reset()
    }

    private fun runRequestedMode() {
        when (intent.getStringExtra(EXTRA_START_MODE)) {
            START_DUNGEON -> {
                de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.start(this)
                DigiWorldAccessibilityService.instance?.showStatusOnly("Dungeon rotation: waiting for Home")
            }
            START_COPILOT -> {
                de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.start()
                when {
                    AutomationState.autoBondRotationEnabled &&
                        de.robinthor.digiworldexplorer.automation.BondCycleTimer.remainingMillis() == 0L ->
                        de.robinthor.digiworldexplorer.feed.BondRotationRequest.start()
                    AutomationState.autoFarmEnabled -> {
                        de.robinthor.digiworldexplorer.automation.BondCycleTimer.requestFarmRecovery()
                        de.robinthor.digiworldexplorer.automation.BondFarmAnalyzer.requestVisit()
                    }
                    AutomationState.copilotRewardsEnabled ->
                        de.robinthor.digiworldexplorer.automation.HomeIdleRewardRequest.start()
                    AutomationState.copilotDwsEnabled ->
                        de.robinthor.digiworldexplorer.automation.DwsExcursionRequest.start()
                    else -> de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.stop("No modules selected")
                }
                DigiWorldAccessibilityService.instance?.showStatusOnly("Digi Co-Pilot: waiting for verified Home")
            }
        }
    }

    private fun restoreOverlays() {
        val settings = getSharedPreferences("settings", Context.MODE_PRIVATE)
        DigiWorldAccessibilityService.instance?.apply {
            setQuickControlsEnabled(settings.getBoolean("quick_overlay_enabled", false))
            setQuickStatusVisible(settings.getBoolean("director_card_visible", true))
            setOverlayEnabled(AutomationState.overlayEnabled)
        }
    }

    private fun finishWithoutAnimation() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun returnToGame() {
        packageManager.getLaunchIntentForPackage(GAME_PACKAGE)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(launch)
        }
    }
}

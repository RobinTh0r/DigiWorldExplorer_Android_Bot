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
import de.robinthor.digiworldexplorer.strategy.DwsNavigationSettings

/** Requests MediaProjection without bringing the full settings UI in front of the game. */
class CaptureConsentActivity : ComponentActivity() {
    private companion object {
        const val GAME_PACKAGE = "com.bandainamcoent.dgup_ww"
    }

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            applyRuntimeSettings()
            AutomationState.stop()
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            ScreenCaptureService.setAutomation(this, true)
            DigiWorldAccessibilityService.instance?.setOverlayEnabled(AutomationState.overlayEnabled)
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
            finishWithoutAnimation()
            return
        }
        val manager = getSystemService(MediaProjectionManager::class.java)
        consent.launch(manager.createScreenCaptureIntent())
    }

    private fun applyRuntimeSettings() {
        val settings = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val supporter = SupporterLicenseManager.load(this) != null
        val neverLeft = supporter && settings.getBoolean("dws_never_left", false)
        AutomationState.overlayEnabled = settings.getBoolean("grid_enabled", true)
        AutomationState.autoPurchaseEnabled = settings.getBoolean("auto_purchase", true)
        AutomationState.autoDungeonEnabled = settings.getBoolean("auto_dungeon", true)
        AutomationState.autoNetworkDefenseEnabled = supporter && settings.getBoolean("auto_network_defense", false)
        AutomationState.autoFeedEnabled = settings.getBoolean("auto_feed", false)
        AutomationState.forceLegacyCaptureMetrics = settings.getBoolean("legacy_capture", true)
        AutomationState.summonTouchCorrection = settings.getBoolean("summon_touch_correction", false)
        AutomationState.dwsNavigationSettings = DwsNavigationSettings(
            allowLeft = !neverLeft,
            forceForwardAttack = neverLeft,
            dashSpamUntilZero = supporter && settings.getBoolean("dws_dash_spam", false),
            collectOnlyEnergy = supporter && settings.getBoolean("dws_only_energy", false),
            betterEnergyCollect = true,
            blindStageFailedTap = true,
        )
        settings.edit().putBoolean("dws_blind_stage_tap", true).apply()
        RewardPurchaseFrameAnalyzer.reset()
        DungeonFrameAnalyzer.reset()
        NetworkDefenseFrameAnalyzer.reset()
        FeedFrameAnalyzer.reset()
        StageFailedFrameAnalyzer.reset()
        CaptureFrameAnalyzer.resetCalibration()
        AutoMoveController.reset()
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

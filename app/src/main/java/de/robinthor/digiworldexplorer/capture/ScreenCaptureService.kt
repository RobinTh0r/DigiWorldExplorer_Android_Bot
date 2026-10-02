package de.robinthor.digiworldexplorer.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.automation.FrameOrchestrator
import de.robinthor.digiworldexplorer.automation.FrameOwner
import de.robinthor.digiworldexplorer.automation.FrameProbe
import de.robinthor.digiworldexplorer.automation.AutomationEventKind
import de.robinthor.digiworldexplorer.automation.AutomationEventLog
import de.robinthor.digiworldexplorer.automation.ScreenDirector
import de.robinthor.digiworldexplorer.automation.PassiveScreenClassifier
import de.robinthor.digiworldexplorer.automation.ObservedScreen
import de.robinthor.digiworldexplorer.automation.GameEntryAnalyzer
import de.robinthor.digiworldexplorer.automation.HomeIdleRewardAnalyzer
import de.robinthor.digiworldexplorer.automation.HomeIdleRewardRequest
import de.robinthor.digiworldexplorer.automation.DwsExcursionAnalyzer
import de.robinthor.digiworldexplorer.automation.DwsExcursionRequest
import de.robinthor.digiworldexplorer.automation.DigiCopilotRequest
import de.robinthor.digiworldexplorer.automation.BondCycleTimer
import de.robinthor.digiworldexplorer.purchase.RewardPurchaseFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.FeedFrameAnalyzer
import de.robinthor.digiworldexplorer.feed.BondRotationAnalyzer
import de.robinthor.digiworldexplorer.feed.StageFailedFrameAnalyzer
import de.robinthor.digiworldexplorer.network.NetworkDefenseFrameAnalyzer
import de.robinthor.digiworldexplorer.dungeon.DungeonFrameAnalyzer
import de.robinthor.digiworldexplorer.runner.GekkomonRunFrameAnalyzer
import de.robinthor.digiworldexplorer.strategy.AutomationState

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var framesSeen = 0
    private var lastRecognizedContent = 0L
    private var missingStatusShown = false
    private var idleStopRequested = false
    private var badCaptureSince = 0L
    private var healthyCaptureSince = 0L
    private var captureImageMissing = false
    private var lastGridRecognized = 0L
    private var lastFrameOwner = FrameOwner.NONE
    private var lastDirectorSnapshot = ScreenDirector.snapshot()
    private var passiveScreen = ObservedScreen.UNKNOWN
    @Volatile private var shuttingDown = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            android.util.Log.w("DigiWorldCapture", "MediaProjection stopped by system or user")
            releaseCapture()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.initialize(this)
        de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.record("CAPTURE", "service created")
        de.robinthor.digiworldexplorer.automation.BondCycleTimer.initialize(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP, ACTION_AUTO_OFF -> {
                AutomationState.stop()
                CaptureSessionState.markCaptureStopped()
                DigiWorldAccessibilityService.instance?.apply {
                    setQuickControlsEnabled(false)
                    clearCalibrationOverlay()
                    setOverlayEnabled(false)
                    showStatusOnly("", false)
                }
                releaseCapture()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_AUTO_ON -> { AutomationState.enabled=true; startCaptureForeground(); return START_NOT_STICKY }
            ACTION_STUCK -> {
                AutomationState.enabled = false
                AutomationState.overlayEnabled = false
                DigiWorldAccessibilityService.instance?.setOverlayEnabled(false)
                showStuckNotification()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startCaptureForeground()
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
            ?: return START_NOT_STICKY
        val resultData = intent.intentExtra(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY
        if (projection == null) {
            beginCapture(resultCode, resultData)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AutomationState.stop(); releaseCapture(); super.onDestroy()
    }
    override fun onTaskRemoved(rootIntent: Intent?) { AutomationState.stop(); releaseCapture(); stopSelf(); super.onTaskRemoved(rootIntent) }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCaptureForeground() {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            1,
            stopIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val autoOffPendingIntent = android.app.PendingIntent.getService(
            this, 2, Intent(this, ScreenCaptureService::class.java).setAction(ACTION_AUTO_OFF),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (AutomationState.enabled) getString(R.string.notification_auto_on) else getString(R.string.notification_auto_off))
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_auto_stop), autoOffPendingIntent)
            .addAction(0, getString(R.string.auto_stop), stopPendingIntent)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginCapture(resultCode: Int, resultData: Intent) {
        shuttingDown = false
        CaptureFrameAnalyzer.resetCalibration()
        lastRecognizedContent = SystemClock.elapsedRealtime()
        missingStatusShown = false
        idleStopRequested = false
        lastGridRecognized = 0L
        lastFrameOwner = FrameOwner.NONE
        ScreenDirector.reset()
        lastDirectorSnapshot = ScreenDirector.snapshot()
        passiveScreen = ObservedScreen.UNKNOWN
        AutomationEventLog.clear()
        AutomationEventLog.record(AutomationEventKind.CAPTURE_STARTED, "CAPTURE_STARTED")
        val manager = getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = manager.getMediaProjection(resultCode, resultData)
        if (mediaProjection == null) {
            android.util.Log.e("DigiWorldCapture", "getMediaProjection returned null (resultCode=$resultCode)")
            return
        }
        mediaProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        val displayMetrics = resources.displayMetrics
        val useLegacyMetrics = Build.VERSION.SDK_INT < 30 || AutomationState.forceLegacyCaptureMetrics
        val width: Int
        val height: Int
        if (useLegacyMetrics) {
            width = displayMetrics.widthPixels.coerceAtLeast(1)
            height = displayMetrics.heightPixels.coerceAtLeast(1)
        } else {
            val bounds = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
            width = bounds.width().coerceAtLeast(1)
            height = bounds.height().coerceAtLeast(1)
        }
        val density = displayMetrics.densityDpi
        android.util.Log.i("DigiWorldCapture", "metrics mode=${if (useLegacyMetrics) "legacy" else "currentWindow"} ${width}x$height")
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val thread = HandlerThread("DigiWorldAnalysis").apply { start() }
        reader.setOnImageAvailableListener({ source ->
            if (shuttingDown) {
                source.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            source.acquireLatestImage()?.use { image ->
                framesSeen++
                var recognized = false
                val featureFrame = framesSeen % 3 == 0
                val captureBlocked = featureFrame && updateCaptureQuality(image, width, height)
                val gameForeground = DigiWorldAccessibilityService.instance?.isGameForeground() == true
                if (!gameForeground) {
                    // Never classify or tap our own settings UI, the launcher, or another app.
                    passiveScreen = ObservedScreen.UNKNOWN
                    GameEntryAnalyzer.reset()
                    publishDirector(ObservedScreen.UNKNOWN)
                    recognized = false
                } else if (captureBlocked) {
                    recognized = captureImageMissing
                    publishDirector(FrameOwner.CAPTURE_BLOCKED)
                } else {
                    val digiCopilotOwns = DigiCopilotRequest.active()
                    val networkFrame = featureFrame || NetworkDefenseFrameAnalyzer.isSessionActive()
                    // Classic V4 Summon must get the frame before the V5 entry/reveal guards.
                    // Otherwise those guards either tap cards as notices or swallow animation taps.
                    val classicSummon = !digiCopilotOwns && AutomationState.autoPurchaseEnabled &&
                        !de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.ownsFrames()
                    val owner = if (classicSummon &&
                        RewardPurchaseFrameAnalyzer.analyze(image, width, height)) {
                        FrameOwner.SUMMON
                    } else if (de.robinthor.digiworldexplorer.purchase.SummonRewardFrameGuard.analyze(image, width, height)) {
                        FrameOwner.SUMMON
                    } else if (de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.ownsFrames()) {
                        // The user-started pass owns ALL frames, including settle/park frames.
                        // Only the rotation may delegate its own reward/battle handlers.
                        if (featureFrame && de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.active())
                            de.robinthor.digiworldexplorer.dungeon.DungeonRotationAnalyzer.analyze(image, width, height)
                        FrameOwner.DUNGEON
                    } else FrameOrchestrator.resolve(false, listOf(
                        FrameProbe(FrameOwner.GAME_ENTRY, enabled = featureFrame && HomeIdleRewardRequest.active()) {
                            HomeIdleRewardAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.WORLD_SEARCH, enabled = featureFrame && DwsExcursionRequest.active()) {
                            DwsExcursionAnalyzer.analyze(image, width, height)
                        },
                        // Login and idle rewards are blocking entry screens and outrank feature tasks.
                        FrameProbe(FrameOwner.GAME_ENTRY, enabled = featureFrame &&
                            (!digiCopilotOwns || !BondCycleTimer.awaitingFarm()) &&
                            !de.robinthor.digiworldexplorer.purchase.RewardPurchaseFrameAnalyzer.isSequenceActive()) {
                            GameEntryAnalyzer.analyze(image, width, height)
                        },
                        // Persistent failure dialogs outrank every task. Network Defense is excluded
                        // because its battle layout may contain similar red/gray regions.
                        FrameProbe(FrameOwner.STAGE_FAILED, enabled = !AutomationState.autoNetworkDefenseEnabled) {
                            val found = StageFailedFrameAnalyzer.analyze(image, width, height)
                            if (found && AutomationState.autoDungeonEnabled) DungeonFrameAnalyzer.onFailureDialogHandled()
                            found
                        },
                        // Active Network runs inspect every frame because the boss banner is brief.
                        FrameProbe(FrameOwner.NETWORK_DEFENSE, enabled = networkFrame && !digiCopilotOwns) {
                            NetworkDefenseFrameAnalyzer.analyze(image, width, height)
                        },
                        // A calibrated World Search grid keeps priority over all menu tasks.
                        FrameProbe(FrameOwner.WORLD_SEARCH, enabled = featureFrame && CaptureFrameAnalyzer.isCalibrated) {
                            val gridFound = CaptureFrameAnalyzer.analyze(this, image, width, height)?.detected == true
                            val now = SystemClock.elapsedRealtime()
                            if (gridFound) {
                                lastGridRecognized = now
                                if (AutomationState.autoFeedEnabled) FeedFrameAnalyzer.pauseForDigiWorld()
                            } else if (lastGridRecognized > 0L && now - lastGridRecognized >= GRID_RELEASE_TIMEOUT) {
                                android.util.Log.i("DigiWorldCapture", "grid absent for ${now - lastGridRecognized} ms - releasing DigiWorld mode")
                                CaptureFrameAnalyzer.resetCalibration()
                                DigiWorldAccessibilityService.instance?.showStatusOnly("", false)
                                lastGridRecognized = 0L
                            }
                            gridFound
                        },
                        FrameProbe(FrameOwner.GEKKOMON_RUN, enabled = AutomationState.autoRunnerEnabled && !digiCopilotOwns) {
                            GekkomonRunFrameAnalyzer.analyze(image, width, height)
                        },
                        // A summon transaction is one visual sequence (menu -> optional prompt ->
                        // reveal -> result). Keep it ahead of generic blue dungeon/reward probes;
                        // otherwise the result grid can alternate between SUMMON and DUNGEON.
                        FrameProbe(FrameOwner.SUMMON, enabled = featureFrame && !digiCopilotOwns && !classicSummon) {
                            RewardPurchaseFrameAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.FARM, enabled = featureFrame && !DungeonFrameAnalyzer.isSessionActive()) {
                            de.robinthor.digiworldexplorer.automation.BondFarmAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.DUNGEON, enabled = featureFrame && de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.active()) {
                            de.robinthor.digiworldexplorer.dungeon.DungeonRotationAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.DUNGEON, enabled = featureFrame && !digiCopilotOwns &&
                            AutomationState.autoDungeonEnabled) {
                            DungeonFrameAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.FARM, enabled = featureFrame && !DungeonFrameAnalyzer.isSessionActive() &&
                            (!digiCopilotOwns || BondCycleTimer.awaitingFarm())) {
                            de.robinthor.digiworldexplorer.farm.FarmHarvestAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.BOND, enabled = (featureFrame || BondRotationAnalyzer.needsFastPolling()) &&
                            !DungeonFrameAnalyzer.isSessionActive()) {
                            de.robinthor.digiworldexplorer.feed.BondRotationAnalyzer.analyze(image, width, height)
                        },
                        FrameProbe(FrameOwner.BOND, enabled = featureFrame && !DungeonFrameAnalyzer.isSessionActive()) {
                            FeedFrameAnalyzer.analyze(image, width, height)
                        },
                        // Initial calibration runs only after no specialized task claimed the frame.
                        FrameProbe(FrameOwner.WORLD_SEARCH, enabled = !CaptureFrameAnalyzer.isCalibrated && framesSeen % 10 == 0) {
                            val found = CaptureFrameAnalyzer.analyze(this, image, width, height)?.detected == true
                            if (found) lastGridRecognized = SystemClock.elapsedRealtime()
                            found
                        },
                    ))
                    if (owner != lastFrameOwner) {
                        AutomationEventLog.record(AutomationEventKind.OWNER_CHANGED, "${lastFrameOwner.name}:${owner.name}")
                        if (owner == FrameOwner.DUNGEON || lastFrameOwner == FrameOwner.DUNGEON || owner == FrameOwner.CAPTURE_BLOCKED)
                            de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.requestScreenshot("owner-${lastFrameOwner.name}-${owner.name}")
                        lastFrameOwner = owner
                    }
                    // Publish only a classification from this frame. The old independent
                    // modulo schedules published UNKNOWN between successful feature scans.
                    if (owner in setOf(FrameOwner.NONE, FrameOwner.BOND) && featureFrame) {
                        passiveScreen = PassiveScreenClassifier.detect(image, width, height)
                    } else if (owner != FrameOwner.NONE) passiveScreen = ObservedScreen.UNKNOWN
                    if (featureFrame || owner != FrameOwner.NONE) {
                    if (owner == FrameOwner.BOND && featureFrame) {
                        publishDirector(passiveScreen)
                    } else if (owner == FrameOwner.BOND) {
                        // No fresh scan: do not overwrite the last observation with the task name.
                    } else if (owner == FrameOwner.FARM && de.robinthor.digiworldexplorer.automation.BondFarmAnalyzer.screen in
                        setOf(ObservedScreen.HOME, ObservedScreen.EXPLORE_MENU)) {
                        publishDirector(de.robinthor.digiworldexplorer.automation.BondFarmAnalyzer.screen)
                    } else if (owner == FrameOwner.NONE) publishDirector(passiveScreen) else publishDirector(owner)
                    }
                    recognized = owner != FrameOwner.NONE || passiveScreen != ObservedScreen.UNKNOWN
                }
                de.robinthor.digiworldexplorer.diagnostics.PersistentDiagnosticLog.captureIfRequested(image, width, height)
                if (recognized) markContentRecognized() else checkRecognitionTimeouts()
            }
        }, Handler(thread.looper))
        val display = mediaProjection.createVirtualDisplay(
            "DigiWorldCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null,
        )
        projection = mediaProjection
        imageReader = reader
        captureThread = thread
        virtualDisplay = display
        CaptureSessionState.markCaptureStarted()
        android.util.Log.i("DigiWorldCapture", "capture started ${width}x$height density=$density display=${display != null}")
    }

    private fun updateCaptureQuality(image: android.media.Image, width: Int, height: Int): Boolean {
        val plane = image.planes.firstOrNull() ?: return false
        if (plane.pixelStride < 3) return false
        val buffer = plane.buffer
        val result = CaptureQualityDetector.detect(width, height) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        val now = SystemClock.elapsedRealtime()
        if (result.quality != CaptureQuality.VALID) {
            healthyCaptureSince = 0L
            if (badCaptureSince == 0L) badCaptureSince = now
            if (now - badCaptureSince >= MISSING_IMAGE_CONFIRMATION) {
                if (!captureImageMissing) android.util.Log.w("DigiWorldCapture", "missing game image quality=${result.quality} mean=${result.meanLuma} deviation=${result.lumaDeviation} chroma=${result.meanChroma}")
                captureImageMissing = true
                DigiWorldAccessibilityService.instance?.showStatusOnly(getString(R.string.overlay_no_capture_image))
            }
            return true
        }
        badCaptureSince = 0L
        if (!captureImageMissing) return false
        if (healthyCaptureSince == 0L) healthyCaptureSince = now
        if (now - healthyCaptureSince < CAPTURE_RECOVERY_CONFIRMATION) return true
        captureImageMissing = false
        healthyCaptureSince = 0L
        DigiWorldAccessibilityService.instance?.showStatusOnly("", false)
        android.util.Log.i("DigiWorldCapture", "structured game image recovered")
        return false
    }
    private fun markContentRecognized() {
        lastRecognizedContent = SystemClock.elapsedRealtime()
        missingStatusShown = false
    }

    private fun publishDirector(owner: FrameOwner) {
        val snapshot = ScreenDirector.observe(owner, AutomationState.enabled)
        if (snapshot != lastDirectorSnapshot) {
            lastDirectorSnapshot = snapshot
            DigiWorldAccessibilityService.instance?.updateDirector(snapshot)
        }
    }

    private fun publishDirector(screen: ObservedScreen) {
        val snapshot = ScreenDirector.observeScreen(screen, AutomationState.enabled)
        if (snapshot != lastDirectorSnapshot) {
            lastDirectorSnapshot = snapshot
            DigiWorldAccessibilityService.instance?.updateDirector(snapshot)
        }
    }

    private fun checkRecognitionTimeouts() {
        if (idleStopRequested || lastRecognizedContent == 0L) return
        val missingFor = SystemClock.elapsedRealtime() - lastRecognizedContent
        if (missingFor >= GRID_HIDE_TIMEOUT && !missingStatusShown) {
            missingStatusShown = true
            // Unknown is useful Director information, not a reason to make the status UI vanish.
            DigiWorldAccessibilityService.instance?.updateDirector(ScreenDirector.snapshot())
        }
        if (missingFor >= IDLE_STOP_TIMEOUT) {
            // A long loading screen, announcement chain, battle transition, or temporarily
            // missing Bond bubble is normal while the game is in front. Stopping projection
            // here makes the overlay look alive while no frames are being analysed anymore.
            // Keep the session alive in-game; the user can still stop it explicitly and the
            // idle timeout remains useful after leaving the game.
            if (DigiWorldAccessibilityService.instance?.isGameForeground() == true) return
            val boundedTaskActive = de.robinthor.digiworldexplorer.automation.DigiCopilotRequest.active() ||
                de.robinthor.digiworldexplorer.dungeon.DungeonRotationRequest.ownsFrames() ||
                de.robinthor.digiworldexplorer.automation.DwsExcursionRequest.active() ||
                de.robinthor.digiworldexplorer.automation.HomeIdleRewardRequest.active()
            if (boundedTaskActive) {
                // These controllers own explicit per-step deadlines. Black/loading frames during
                // stage changes must not kill MediaProjection and the whole multi-step run.
                return
            }
            idleStopRequested = true
            Handler(Looper.getMainLooper()).post {
                AutomationState.stop()
                showIdleNotification()
                stopSelf()
            }
        }
    }

    private fun releaseCapture() {
        shuttingDown = true
        AutomationEventLog.record(AutomationEventKind.CAPTURE_STOPPED, "CAPTURE_STOPPED")
        CaptureSessionState.markCaptureStopped()
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        captureThread?.quit()
        captureThread = null
        framesSeen = 0
        badCaptureSince = 0L
        healthyCaptureSince = 0L
        captureImageMissing = false
        lastGridRecognized = 0L
        lastFrameOwner = FrameOwner.NONE
        passiveScreen = ObservedScreen.UNKNOWN
        RewardPurchaseFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.purchase.SummonRewardFrameGuard.reset()
        GameEntryAnalyzer.reset()
        DungeonFrameAnalyzer.reset()
        GekkomonRunFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.farm.FarmHarvestAnalyzer.reset()
        de.robinthor.digiworldexplorer.automation.BondFarmAnalyzer.reset()
        de.robinthor.digiworldexplorer.automation.HomeIdleRewardAnalyzer.reset()
        de.robinthor.digiworldexplorer.automation.DwsExcursionAnalyzer.reset()
        NetworkDefenseFrameAnalyzer.reset()
        FeedFrameAnalyzer.reset()
        de.robinthor.digiworldexplorer.feed.BondRotationAnalyzer.reset()
        StageFailedFrameAnalyzer.reset()
        ScreenDirector.reset()
        lastDirectorSnapshot = ScreenDirector.snapshot()
        CaptureFrameAnalyzer.resetCalibration()
        val activeProjection = projection
        projection = null
        activeProjection?.unregisterCallback(projectionCallback)
        activeProjection?.stop()
        DigiWorldAccessibilityService.instance?.showStatusOnly("", false)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showIdleNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.notification_idle_title))
            .setContentText(getString(R.string.notification_idle_body))
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(IDLE_NOTIFICATION_ID, notification)
    }

    private fun showStuckNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.notification_stuck_title))
            .setContentText(getString(R.string.notification_stuck_body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(STUCK_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1001
        private const val STUCK_NOTIFICATION_ID = 1002
        private const val IDLE_NOTIFICATION_ID = 1003
        private const val GRID_HIDE_TIMEOUT = 3_000L
        private const val GRID_RELEASE_TIMEOUT = 1_500L
        private const val MISSING_IMAGE_CONFIRMATION = 2_000L
        private const val CAPTURE_RECOVERY_CONFIRMATION = 750L
        private const val IDLE_STOP_TIMEOUT = 60_000L
        private const val ACTION_START = "capture.start"
        private const val ACTION_STOP = "capture.stop"
        private const val ACTION_AUTO_ON = "capture.auto.on"
        private const val ACTION_AUTO_OFF = "capture.auto.off"
        private const val ACTION_STUCK = "capture.stuck"
        private const val EXTRA_RESULT_CODE = "capture.resultCode"
        private const val EXTRA_RESULT_DATA = "capture.resultData"

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            CaptureSessionState.markCaptureStopped()
            context.startService(
                Intent(context, ScreenCaptureService::class.java).setAction(ACTION_STOP),
            )
        }

        fun setAutomation(context: Context, enabled: Boolean) {
            context.startService(Intent(context, ScreenCaptureService::class.java).setAction(if (enabled) ACTION_AUTO_ON else ACTION_AUTO_OFF))
        }

        fun stopForStuck(context: Context) {
            context.startService(Intent(context, ScreenCaptureService::class.java).setAction(ACTION_STUCK))
        }

        @Suppress("DEPRECATION")
        private fun Intent.intentExtra(name: String): Intent? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(name, Intent::class.java)
            else getParcelableExtra(name)
    }
}

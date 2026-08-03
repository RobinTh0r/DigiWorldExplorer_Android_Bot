package de.robinthor.digiworldexplorer.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutomationState

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var framesSeen = 0

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            android.util.Log.w("DigiWorldCapture", "MediaProjection stopped by system or user")
            releaseCapture()
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { AutomationState.stop(); stopSelf(); return START_NOT_STICKY }
            ACTION_AUTO_ON -> { AutomationState.enabled=true; startCaptureForeground(); return START_NOT_STICKY }
            ACTION_AUTO_OFF -> { AutomationState.stop(); startCaptureForeground(); return START_NOT_STICKY }
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
    override fun onTaskRemoved(rootIntent: Intent?) { AutomationState.stop(); stopSelf(); super.onTaskRemoved(rootIntent) }

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
            .addAction(0, getString(R.string.stop_all), stopPendingIntent)
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
        CaptureFrameAnalyzer.resetCalibration()
        val manager = getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = manager.getMediaProjection(resultCode, resultData)
        if (mediaProjection == null) {
            android.util.Log.e("DigiWorldCapture", "getMediaProjection returned null (resultCode=$resultCode)")
            return
        }
        mediaProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        val bounds = metrics.bounds
        val width = bounds.width().coerceAtLeast(1)
        val height = bounds.height().coerceAtLeast(1)
        val density = resources.configuration.densityDpi
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val thread = HandlerThread("DigiWorldAnalysis").apply { start() }
        reader.setOnImageAvailableListener({ source ->
            source.acquireLatestImage()?.use { image ->
                framesSeen++
                if (CaptureFrameAnalyzer.isCalibrated) {
                    // Kalibriert: das Overlay stoert nicht mehr, also so oft wie moeglich analysieren.
                    // Der Takt bestimmt direkt, wie schnell der Bot laufen kann.
                    if (framesSeen % 3 == 0) CaptureFrameAnalyzer.analyze(this, image, width, height)
                } else {
                    if (framesSeen % 10 == 4) DigiWorldAccessibilityService.instance?.hideForCapture()
                    if (framesSeen % 10 == 0) CaptureFrameAnalyzer.analyze(this, image, width, height)
                }
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
        android.util.Log.i("DigiWorldCapture", "capture started ${width}x$height density=$density display=${display != null}")
    }

    private fun releaseCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        captureThread?.quitSafely()
        captureThread = null
        framesSeen = 0
        CaptureFrameAnalyzer.resetCalibration()
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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

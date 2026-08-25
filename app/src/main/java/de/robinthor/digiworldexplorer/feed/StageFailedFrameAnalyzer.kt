package de.robinthor.digiworldexplorer.feed

import android.graphics.Color
import android.media.Image
import android.os.SystemClock
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.strategy.AutomationState

/** Global, language-independent detector for the Stage Failed growth-guide dialog. */
object StageFailedFrameAnalyzer {
    private const val SCAN_INTERVAL_MS = 2_000L
    private const val TAP_RETRY_MS = 2_000L
    private var nextScanAt = 0L
    private var nextTapAt = 0L
    private const val REQUIRED_CONFIRMATIONS = 2
    private var dialogWasVisible = false
    private var confirmations = 0

    fun analyze(image: Image, width: Int, height: Int): Boolean {
        val now = SystemClock.elapsedRealtime()
        // No extra capture loop: this runs on the existing MediaProjection stream. Between scans,
        // retain exclusive ownership only while a failure dialog was positively detected.
        if (now < nextScanAt) return dialogWasVisible
        nextScanAt = now + SCAN_INTERVAL_MS

        val plane = image.planes.firstOrNull() ?: return dialogWasVisible
        if (plane.pixelStride < 3) return dialogWasVisible
        val buffer = plane.buffer
        fun pixel(x: Int, y: Int): Int {
            val offset = y * plane.rowStride + x * plane.pixelStride
            return Color.rgb(buffer.get(offset).toInt() and 255, buffer.get(offset + 1).toInt() and 255, buffer.get(offset + 2).toInt() and 255)
        }
        fun ratio(x0: Double, y0: Double, x1: Double, y1: Double, match: (Int, Int, Int) -> Boolean): Double {
            val step = (width / 240).coerceAtLeast(2)
            var hits = 0
            var total = 0
            for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
                for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                    val c = pixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
                    if (match(Color.red(c), Color.green(c), Color.blue(c))) hits++
                    total++
                }
            }
            return hits / total.coerceAtLeast(1).toDouble()
        }

        val headerRed = ratio(.08, .11, .92, .23) { r, g, b -> r > 120 && r > g * 1.30 && r > b * 1.15 }
        val panelGray = ratio(.08, .21, .92, .30) { r, g, b -> r > 90 && kotlin.math.abs(r-g) < 28 && kotlin.math.abs(g-b) < 28 }
        val guideRed = ratio(.10, .28, .90, .82) { r, g, b -> r > 105 && r > g * 1.25 && r > b * 1.10 }
        // The dismiss area differs widely between devices and game layouts (including the
        // VS/Tower loss screen), so it must not be used as a recognition requirement.
        // The red failure title, neutral guide header and red guide panels are the stable trio.
        val detected = headerRed >= .08 && panelGray >= .28 && guideRed >= .02
        if (!detected) {
            confirmations = 0
            if (dialogWasVisible) FeedFrameAnalyzer.allowImmediateScan()
            dialogWasVisible = false
            return false
        }

        confirmations++
        if (confirmations < REQUIRED_CONFIRMATIONS) return false

        dialogWasVisible = true
        DigiWorldAccessibilityService.instance?.let {
            it.updateStatusKeepingGrid(it.getString(R.string.overlay_stage_failed), true)
        }
        if (now >= nextTapAt && AutomationState.enabled) {
            nextTapAt = now + TAP_RETRY_MS
            // Same resolution-independent safe area below the guide and above the home button.
            DigiWorldAccessibilityService.instance?.dispatchNormalizedTap(.50f, .84f) { }
        }
        return true
    }

    /** Pure detector so an already confirmed automation mode can close this dialog immediately. */
    fun detect(width: Int, height: Int, pixel: (Int, Int) -> Int): Boolean {
        fun ratio(x0: Double, y0: Double, x1: Double, y1: Double, match: (Int, Int, Int) -> Boolean): Double {
            val step = (width / 240).coerceAtLeast(2)
            var hits = 0
            var total = 0
            for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
                for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                    val c = pixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
                    if (match(Color.red(c), Color.green(c), Color.blue(c))) hits++
                    total++
                }
            }
            return hits / total.coerceAtLeast(1).toDouble()
        }
        val headerRed = ratio(.08, .11, .92, .23) { r, g, b -> r > 120 && r > g * 1.30 && r > b * 1.15 }
        val panelGray = ratio(.08, .21, .92, .30) { r, g, b -> r > 90 && kotlin.math.abs(r-g) < 28 && kotlin.math.abs(g-b) < 28 }
        val guideRed = ratio(.10, .28, .90, .82) { r, g, b -> r > 105 && r > g * 1.25 && r > b * 1.10 }
        return headerRed >= .08 && panelGray >= .28 && guideRed >= .02
    }
    fun reset() {
        nextScanAt = 0L
        nextTapAt = 0L
        dialogWasVisible = false
        confirmations = 0
    }
}

package de.robinthor.digiworldexplorer.vision

import kotlin.math.max
import kotlin.math.min

fun interface ArgbReader {
    fun argbAt(x: Int, y: Int): Int
}

/** Allocation-light view over a frame. Coordinates are always clamped at the frame edge. */
class PixelFrame(
    val width: Int,
    val height: Int,
    private val reader: ArgbReader,
) {
    init {
        require(width > 0 && height > 0)
    }

    fun argbAt(x: Int, y: Int): Int = reader.argbAt(
        x.coerceIn(0, width - 1),
        y.coerceIn(0, height - 1),
    )

    fun rgbAt(x: Int, y: Int): Rgb {
        val value = argbAt(x, y)
        return Rgb(value shr 16 and 255, value shr 8 and 255, value and 255)
    }
    val observedViewport:GameViewport by lazy {
        val canonical=de.robinthor.digiworldexplorer.capture.FrameGeometryRegistry.callerAnalysisSize()
        if(canonical?.width==width && canonical.height==height) return@lazy GameViewport.fit(width,height)
        val area=de.robinthor.digiworldexplorer.capture.VisibleGameArea.detect(this)
        if(area==null)GameViewport.fit(width,height) else GameViewport(area.left,area.top,area.width,area.height)
    }
}

data class Rgb(val red: Int, val green: Int, val blue: Int) {
    fun hsv(): Hsv {
        val high = max(red, max(green, blue))
        val low = min(red, min(green, blue))
        val delta = high - low
        val hueDegrees = when {
            delta == 0 -> 0.0
            high == red -> 60.0 * (((green - blue).toDouble() / delta) % 6.0)
            high == green -> 60.0 * (((blue - red).toDouble() / delta) + 2.0)
            else -> 60.0 * (((red - green).toDouble() / delta) + 4.0)
        }.let { if (it < 0.0) it + 360.0 else it }
        val saturation = if (high == 0) 0 else (delta * 255.0 / high).toInt()
        // OpenCV represents hue as 0..179. Keep the same scale as the reference thresholds.
        return Hsv((hueDegrees / 2.0).toInt().coerceIn(0, 179), saturation.coerceIn(0, 255), high)
    }
}

data class Hsv(val hue: Int, val saturation: Int, val value: Int)

data class NormalizedPoint(val x: Double, val y: Double)

data class GameViewport(val left: Int, val top: Int, val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0)
    }

    fun pixel(point: NormalizedPoint): Pair<Int, Int> =
        (left + (point.x.coerceIn(0.0, 1.0) * (width - 1)).toInt()) to
            (top + (point.y.coerceIn(0.0, 1.0) * (height - 1)).toInt())

    /** Adaptive phone UI used by current 19.5:9 and 20:9 Galaxy, Pixel and OnePlus devices. */
    val usesTallPhoneLayout: Boolean get() = width.toDouble() / height < .53

    companion object {
        /** Dimensions alone cannot prove pillar/letterboxing. Runtime images are already
         * cropped using Android window bounds and observed content; never invent 9:16. */
        fun fit(frameWidth: Int, frameHeight: Int)=GameViewport(0,0,frameWidth,frameHeight)
        fun detect(frame:PixelFrame)=frame.observedViewport
    }
}

fun PixelFrame.ratioInViewportPatch(
    viewport: GameViewport,
    center: NormalizedPoint,
    halfWidth: Double,
    halfHeight: Double,
    step: Int = 2,
    predicate: (Rgb) -> Boolean,
): Double {
    val (cx, cy) = viewport.pixel(center)
    val radiusX = (viewport.width * halfWidth).toInt().coerceAtLeast(1)
    val radiusY = (viewport.height * halfHeight).toInt().coerceAtLeast(1)
    var matches = 0
    var total = 0
    for (y in cy - radiusY..cy + radiusY step step.coerceAtLeast(1)) {
        for (x in cx - radiusX..cx + radiusX step step.coerceAtLeast(1)) {
            total++
            if (predicate(rgbAt(x, y))) matches++
        }
    }
    return matches.toDouble() / total.coerceAtLeast(1)
}

package de.robinthor.digiworldexplorer.vision

data class NormalizedRect(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width get() = right - left
    val height get() = bottom - top
    val center get() = NormalizedPoint((left + right) / 2.0, (top + bottom) / 2.0)
}

/** Finds connected visible regions inside a bounded viewport area. */
object ColorRegionLocator {
    fun find(
        frame: PixelFrame,
        viewport: GameViewport,
        area: NormalizedRect,
        sampleStep: Int = 2,
        predicate: (Rgb) -> Boolean,
    ): List<NormalizedRect> {
        val step = sampleStep.coerceAtLeast(1)
        val x0 = (viewport.left + viewport.width * area.left).toInt().coerceIn(viewport.left, viewport.left + viewport.width - 1)
        val y0 = (viewport.top + viewport.height * area.top).toInt().coerceIn(viewport.top, viewport.top + viewport.height - 1)
        val x1 = (viewport.left + viewport.width * area.right).toInt().coerceIn(x0 + 1, viewport.left + viewport.width)
        val y1 = (viewport.top + viewport.height * area.bottom).toInt().coerceIn(y0 + 1, viewport.top + viewport.height)
        val maskWidth = ((x1 - x0) + step - 1) / step
        val maskHeight = ((y1 - y0) + step - 1) / step
        val mask = BooleanArray(maskWidth * maskHeight) { index ->
            val x = (x0 + (index % maskWidth) * step).coerceAtMost(x1 - 1)
            val y = (y0 + (index / maskWidth) * step).coerceAtMost(y1 - 1)
            predicate(frame.rgbAt(x, y))
        }
        return ColorComponents.find(mask, maskWidth, maskHeight).map { component ->
            NormalizedRect(
                ((x0 + component.left * step) - viewport.left).toDouble() / viewport.width,
                ((y0 + component.top * step) - viewport.top).toDouble() / viewport.height,
                ((x0 + (component.right + 1) * step) - viewport.left).toDouble() / viewport.width,
                ((y0 + (component.bottom + 1) * step) - viewport.top).toDouble() / viewport.height,
            )
        }
    }
}

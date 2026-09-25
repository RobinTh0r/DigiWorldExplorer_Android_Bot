package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*

data class FarmDialogDetection(
    val view: FarmView = FarmView.UNKNOWN,
    val slots: List<NormalizedPoint> = emptyList(),
    val seedCounts: List<Int?> = emptyList(),
    val selectedSlot: Int? = null,
    val selectButton: NormalizedPoint? = null,
    val closeTarget: NormalizedPoint? = null,
)

/**
 * Independently implemented component recognition using the reference's observed HSV ranges and
 * geometry. Requires visible field context. Slot appearance does NOT establish that a seed is free;
 * a separate counter/cost reader must provide that evidence to FarmController.
 */
object FarmDialogDetector {
    private const val WIDTH = 360
    private const val HEIGHT = 640

    fun detect(
        frame: PixelFrame,
        visiblePlots: Int,
        viewport: GameViewport = GameViewport.fit(frame.width, frame.height),
        trustedFarmFlow: Boolean = false,
    ): FarmDialogDetection {
        if (!trustedFarmFlow && visiblePlots !in 2..6) return FarmDialogDetection()
        val slots = BooleanArray(WIDTH * HEIGHT)
        val brackets = BooleanArray(slots.size)
        val buttons = BooleanArray(slots.size)
        val errorPanels = BooleanArray(slots.size)
        val errorButtons = BooleanArray(slots.size)
        for (y in 0 until HEIGHT) for (x in 0 until WIDTH) {
            val px = viewport.left + x * viewport.width / WIDTH
            val py = viewport.top + y * viewport.height / HEIGHT
            val hsv = frame.rgbAt(px, py).hsv()
            val i = y * WIDTH + x
            slots[i] = hsv.hue in 0..15 && hsv.saturation in 140..220 && hsv.value in 60..160
            brackets[i] = hsv.hue in 8..22 && hsv.saturation >= 180 && hsv.value >= 180
            buttons[i] = hsv.hue in 45..65 && hsv.saturation >= 100 && hsv.value >= 150
            errorPanels[i] = hsv.hue in 100..120 && hsv.saturation >= 100 && hsv.value in 50..190
            errorButtons[i] = hsv.hue in 85..110 && hsv.saturation >= 100 && hsv.value >= 150
        }
        fun blobs(mask: BooleanArray, share: Double) = ColorComponents.find(mask, WIDTH, HEIGHT)
            .filter { it.pixels.toDouble() / mask.size >= share }
        fun center(blob: ColorComponent) = NormalizedPoint(
            (blob.left + blob.width / 2.0) / WIDTH,
            (blob.top + blob.height / 2.0) / HEIGHT,
        )
        val seedSlots = blobs(slots, .003).filter { center(it).y in .44.. .52 }.sortedBy { it.left }
        val greenButtons = blobs(buttons, .003)
        if (trustedFarmFlow) {
            val panel = blobs(errorPanels, .05).maxByOrNull { it.pixels }
            val confirm = blobs(errorButtons, .002).maxByOrNull { it.pixels }
            if (panel != null && confirm != null)
                return FarmDialogDetection(FarmView.ERROR, closeTarget = center(confirm))
        }
        val select = greenButtons.maxByOrNull { it.pixels }
        if (seedSlots.size == 3 && select != null) {
            val bracketCounts = seedSlots.map { slot ->
                val left = slot.left; val right = slot.right; val top = slot.top; val bottom = slot.bottom
                (top - 8..bottom + 8).sumOf { y ->
                    (left - 8..right + 8).count { x ->
                        x in 0 until WIDTH && y in 0 until HEIGHT &&
                            minOf(kotlin.math.abs(x - left), kotlin.math.abs(x - right),
                                kotlin.math.abs(y - top), kotlin.math.abs(y - bottom)) <= 7 &&
                            brackets[y * WIDTH + x]
                    }
                }
            }
            val best = bracketCounts.indices.maxByOrNull { bracketCounts[it] }
            val runnerUp = bracketCounts.sortedDescending().getOrElse(1) { 0 }
            val selected = best?.takeIf { bracketCounts[it] >= 40 && bracketCounts[it] >= runnerUp * 2 }
            return FarmDialogDetection(
                view = FarmView.SEEDS,
                slots = seedSlots.map(::center),
                seedCounts = FarmResourceReaders.dialogSeeds(frame, viewport),
                selectedSlot = selected,
                selectButton = center(select),
            )
        }
        if (seedSlots.size != 3 && greenButtons.any { it.width.toDouble() / it.height >= 3.0 }) {
            val waterButton = greenButtons.maxByOrNull { it.pixels }
            return FarmDialogDetection(
                view = FarmView.WATER,
                selectButton = waterButton?.let(::center),
                closeTarget = NormalizedPoint(.5, .1),
            )
        }
        return FarmDialogDetection()
    }
}

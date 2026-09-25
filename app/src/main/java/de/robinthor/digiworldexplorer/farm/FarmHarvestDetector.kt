package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*
import kotlin.math.abs

data class FarmHarvestDetection(
    val field: Boolean,
    val states: List<PlotState>,
    val bubbleBlocked: Set<Int> = emptySet(),
    val visiblePlots: Int = 0,
    val waterablePlots: Set<Int> = emptySet(),
    val wateringPriorities: Map<Int, Int> = emptyMap(),
)

/** Component/number-shape recognition for harvest only. No OCR result is treated as a free seed. */
object FarmHarvestDetector {
    private const val W = 360
    private const val H = 640
    val centers = listOf(.507, .653, .810).flatMap { y -> listOf(.313, .640).map { x -> NormalizedPoint(x, y) } }

    fun detect(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): FarmHarvestDetection {
        val ground = BooleanArray(W * H)
        val badges = BooleanArray(W * H)
        val bright = BooleanArray(W * H)
        val bubbles = BooleanArray(W * H)
        val locks = BooleanArray(W * H)
        val progress = BooleanArray(W * H)
        val ripeBubbles = BooleanArray(W * H)
        for (y in 0 until H) for (x in 0 until W) {
            val rgb = frame.rgbAt(viewport.left + x * viewport.width / W, viewport.top + y * viewport.height / H)
            val hsv = rgb.hsv()
            val i = y * W + x
            ground[i] = hsv.hue in 0..20 && hsv.saturation >= 90 && hsv.value >= 40
            badges[i] = hsv.hue in 30..45 && hsv.saturation >= 180 && hsv.value in 20..140
            bright[i] = .299 * rgb.red + .587 * rgb.green + .114 * rgb.blue >= 200
            bubbles[i] = hsv.hue in 88..115 && hsv.saturation >= 35 && hsv.value >= 180
            locks[i] = hsv.hue in 20..42 && hsv.saturation >= 120 && hsv.value >= 150
            progress[i] = hsv.hue in 35..75 && hsv.saturation >= 100 && hsv.value >= 80
            ripeBubbles[i] = hsv.saturation <= 85 && hsv.value >= 180
        }
        val plots = ColorComponents.find(ground, W, H).filter { it.pixels.toDouble() / ground.size in .01.. .15 }
        // Each expected plot must have its own component; a plain orange screen is not a field.
        val matched = centers.map { center -> plots.withIndex().filter { (_, blob) ->
            center.x in (blob.left.toDouble() / W - .06)..((blob.right + 1.0) / W + .06) &&
                center.y in (blob.top.toDouble() / H - .06)..((blob.bottom + 1.0) / H + .06)
        }.minByOrNull { (_, blob) ->
            abs((blob.left + blob.width / 2.0) / W - center.x) + abs((blob.top + blob.height / 2.0) / H - center.y)
        }?.index }
        val visiblePlots = matched.count { it != null }
        // Animated Digimon and the perspective can visually join two neighbouring soil areas.
        // All six anchors must still hit soil and at least four independent areas must remain;
        // this accepts the live field but continues to reject a uniform orange screen.
        if (matched.any { it == null } || matched.filterNotNull().toSet().size < 4)
            return FarmHarvestDetection(false, emptyList(), visiblePlots = visiblePlots)
        val badgeBlobs = ColorComponents.find(badges, W, H).filter { it.pixels.toDouble() / badges.size >= .0012 }
        val fieldBubbleBlobs = ColorComponents.find(ripeBubbles, W, H).filter {
            it.pixels.toDouble() / ripeBubbles.size in .003.. .035 &&
                it.width.toDouble() / W in .10.. .30 && it.height.toDouble() / H in .045.. .18
        }
        val selected = centers.map { center -> badgeBlobs.filter { badge ->
            abs((badge.left + badge.width / 2.0) / W - center.x) <= .12 &&
                (badge.top + badge.height / 2.0) / H in (center.y - .16)..(center.y + .02)
        }.maxByOrNull { it.pixels } }
        val states = selected.mapIndexed { index, badge ->
            val center = centers[index]
            val cx = (center.x * W).toInt()
            val cy = (center.y * H).toInt()
            val lockPixels = (cy - 35..cy + 35).sumOf { y ->
                (cx - 35..cx + 35).count { x -> x in 0 until W && y in 0 until H && locks[y * W + x] }
            }
            val progressPixels = (cy + 12..cy + 34).sumOf { y ->
                (cx - 42..cx + 42).count { x -> x in 0 until W && y in 0 until H && progress[y * W + x] }
            }
            fun pointsAtPlot(bubble: ColorComponent) =
                abs((bubble.left + bubble.width / 2.0) / W - center.x) <= .09 &&
                    (bubble.top + bubble.height / 2.0) / H in (center.y - .11)..(center.y - .015)
            val bubble = fieldBubbleBlobs.firstOrNull(::pointsAtPlot)
            val harvestInkRatio = bubble?.let {
                var ink = 0
                for (y in it.top..it.bottom) for (x in it.left..it.right) if (ground[y * W + x]) ink++
                ink.toDouble() / (it.width * it.height)
            } ?: 0.0
            val amountTextRatio = bubble?.let {
                var dark = 0
                var total = 0
                val left = it.left + (it.width * .15).toInt()
                val right = it.left + (it.width * .85).toInt()
                val top = it.top + (it.height * .58).toInt()
                val bottom = it.top + (it.height * .86).toInt()
                for (y in top..bottom) for (x in left..right) {
                    val rgb = frame.rgbAt(
                        viewport.left + x * viewport.width / W,
                        viewport.top + y * viewport.height / H,
                    )
                    total++
                    if (.299 * rgb.red + .587 * rgb.green + .114 * rgb.blue < 80) dark++
                }
                dark.toDouble() / total.coerceAtLeast(1)
            } ?: 0.0
            // Live yield bubbles (x107, x258 and x1.075) occupy 35.6–39.0% of their
            // pale bubble with meat/ground-coloured ink. The shovel in an empty-plot
            // bubble can reach the old 23.5% limit, so keep a clear margin above it.
            val ripeBubble = bubble != null && (harvestInkRatio >= .34 || amountTextRatio >= .14)
            val emptyBubble = bubble != null && !ripeBubble
            if (ripeBubble) PlotState.RIPE
            else if (emptyBubble) PlotState.EMPTY
            else if (badge == null && progressPixels >= 12) PlotState.GROWING
            else if (badge == null && lockPixels >= 100) PlotState.LOCKED
            else if (badge == null || selected.count { it == badge } > 1) PlotState.UNKNOWN
            else {
                val crop = BooleanArray(badge.width * badge.height) { i -> bright[(badge.top + i / badge.width) * W + badge.left + i % badge.width] }
                val glyphs = ColorComponents.find(crop, badge.width, badge.height).filter { it.height >= 6 && it.width.toDouble() / it.height in .4.. .85 }
                val hasNumber = glyphs.any { a -> glyphs.any { b ->
                    b.left > a.right && b.left - a.right - 1 <= minOf(a.width, b.width) &&
                        minOf(a.height, b.height) >= maxOf(a.height, b.height) * .85 && abs(a.top - b.top) <= maxOf(a.height, b.height) * .25
                } }
                if (hasNumber) PlotState.RIPE else if (crop.none { it }) PlotState.EMPTY else PlotState.UNKNOWN
            }
        }.mapIndexed { index, state ->
            if (state == PlotState.UNKNOWN && selected[index] == null && PlotTimerReader.read(frame, index, viewport) != null)
                PlotState.GROWING else state
        }
        val minBubblePixels = (90.0 * W * H / (viewport.width.toDouble() * viewport.height)).toInt().coerceAtLeast(4)
        val bubbleCenters = ColorComponents.find(bubbles, W, H)
            .filter { it.pixels >= minBubblePixels }
            .map { NormalizedPoint((it.left + it.width / 2.0) / W, (it.top + it.height / 2.0) / H) }
        val blocked = centers.indices.filterTo(mutableSetOf()) { index ->
            val target = target(index)
            bubbleCenters.any { point -> kotlin.math.hypot(point.x - target.x, point.y - target.y) < .06 }
        }
        val waterable = centers.indices.filterTo(mutableSetOf()) { index ->
            val target = waterTarget(index)
            bubbleCenters.any { point -> kotlin.math.hypot(point.x - target.x, point.y - target.y) < .075 }
        }
        val wateringPriorities = waterable.associateWith { index ->
            val center = centers[index]
            var purple = 0
            var red = 0
            // Inspect only the Palmon body/hair. The outer cyan water bubble and the
            // red-brown soil would otherwise dominate the colour score.
            val left = ((center.x - .06) * W).toInt()
            val right = ((center.x + .06) * W).toInt()
            val top = ((center.y - .09) * H).toInt()
            val bottom = ((center.y + .03) * H).toInt()
            for (y in top..bottom) for (x in left..right) {
                if (x !in 0 until W || y !in 0 until H) continue
                val hsv = frame.rgbAt(viewport.left + x * viewport.width / W, viewport.top + y * viewport.height / H).hsv()
                if (hsv.hue in 118..150 && hsv.saturation >= 90 && hsv.value >= 80) purple++
                if ((hsv.hue <= 8 || hsv.hue >= 153) && hsv.saturation >= 105 && hsv.value >= 100) red++
            }
            when {
                purple >= 60 -> 3 // purple Palmon
                red >= 350 -> 2 // red Palmon
                else -> 1 // Mini Palmon
            }
        }
        return FarmHarvestDetection(
            field = true,
            // The water bubble belongs to a growing plot even when its animated sprite
            // obscures the timer or briefly resembles the yellow lock.
            states = states.mapIndexed { index, state ->
                if (index in waterable && state in setOf(PlotState.UNKNOWN, PlotState.LOCKED))
                    PlotState.GROWING else state
            },
            bubbleBlocked = blocked,
            visiblePlots = visiblePlots,
            waterablePlots = waterable,
            wateringPriorities = wateringPriorities,
        )
    }

    fun target(index: Int): NormalizedPoint {
        require(index in centers.indices)
        val center = centers[index]
        return center.copy(x = center.x + if (index % 2 == 0) .07 else -.07)
    }

    fun waterTarget(index: Int): NormalizedPoint {
        require(index in centers.indices)
        val center = centers[index]
        return NormalizedPoint(
            center.x + if (index % 2 == 0) -.105 else .15,
            center.y - .08,
        )
    }
}

package de.robinthor.digiworldexplorer.runner

import kotlin.math.max
import kotlin.math.min

enum class RunnerAction { NONE, JUMP, SLIDE }

data class RunnerDetection(
    val active: Boolean,
    val action: RunnerAction,
    val obstacleX: Double = 1.0,
    val confidence: Double = 0.0,
)

/**
 * Resolution-independent Gekkomon Run detector.
 *
 * The runner uses saturated red/magenta obstacle silhouettes.  We only inspect the game lane and
 * additionally require the green/pink Fever bar above it.  This deliberately ignores event menus
 * and reward dialogs so the detector cannot start a run or press unrelated buttons by itself.
 */
object GekkomonRunScreenDetector {
    private const val LANE_TOP = .30
    private const val LANE_BOTTOM = .72
    private const val LOOK_AHEAD = .36
    private const val TRIGGER_X = .70

    fun detect(width: Int, height: Int, argbAt: (Int, Int) -> Int): RunnerDetection {
        if (width < 1 || height < 1) return RunnerDetection(false, RunnerAction.NONE)
        val step = (width / 270).coerceAtLeast(2)
        // The Fever colours also occur in normal HUD counters. The live minigame always exposes
        // its large purple pause control in the upper-right corner; require both independent
        // anchors before the runner is allowed to claim the screen.
        val pauseShare = ratio(width, height, .765, .045, .895, .125, step, argbAt, ::isPausePurple)
        if (pauseShare < .16) return RunnerDetection(false, RunnerAction.NONE, confidence = pauseShare)
        val feverShare = ratio(width, height, .20, .15, .83, .19, step, argbAt, ::isFever)
        if (feverShare < .055) return RunnerDetection(false, RunnerAction.NONE, confidence = feverShare)

        val x0 = (width * LOOK_AHEAD).toInt()
        val x1 = width
        val y0 = (height * LANE_TOP).toInt()
        val y1 = (height * LANE_BOTTOM).toInt()
        val cols = ((x1 - x0 + step - 1) / step).coerceAtLeast(1)
        val rows = ((y1 - y0 + step - 1) / step).coerceAtLeast(1)
        val mask = BooleanArray(cols * rows)
        for (row in 0 until rows) for (col in 0 until cols) {
            val x = min(width - 1, x0 + col * step)
            val y = min(height - 1, y0 + row * step)
            val c = argbAt(x, y)
            mask[row * cols + col] = isObstacle(c shr 16 and 255, c shr 8 and 255, c and 255)
        }

        val seen = BooleanArray(mask.size)
        var best: Component? = null
        val queue = IntArray(mask.size)
        for (start in mask.indices) {
            if (!mask[start] || seen[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            seen[start] = true
            var count = 0
            var minCol = cols
            var maxCol = 0
            var minRow = rows
            var maxRow = 0
            while (head < tail) {
                val index = queue[head++]
                val row = index / cols
                val col = index % cols
                count++
                minCol = min(minCol, col); maxCol = max(maxCol, col)
                minRow = min(minRow, row); maxRow = max(maxRow, row)
                for ((dc, dr) in NEIGHBOURS) {
                    val nc = col + dc; val nr = row + dr
                    if (nc !in 0 until cols || nr !in 0 until rows) continue
                    val next = nr * cols + nc
                    if (mask[next] && !seen[next]) { seen[next] = true; queue[tail++] = next }
                }
            }
            val share = count.toDouble() / (cols * rows)
            if (share < .0008) continue
            val component = Component(
                left = (x0 + minCol * step).toDouble() / width,
                right = (x0 + (maxCol + 1) * step).toDouble() / width,
                top = (y0 + minRow * step).toDouble() / height,
                bottom = (y0 + (maxRow + 1) * step).toDouble() / height,
                share = share,
            )
            if (best == null || component.left < best.left) best = component
        }

        val obstacle = best ?: return RunnerDetection(true, RunnerAction.NONE, confidence = feverShare)
        if (obstacle.left > TRIGGER_X) return RunnerDetection(true, RunnerAction.NONE, obstacle.left, feverShare + obstacle.share)
        // Floating or tall obstacles require sliding; low, short obstacles require jumping.
        val action = if (obstacle.bottom < .58 || obstacle.bottom - obstacle.top > .18) RunnerAction.SLIDE else RunnerAction.JUMP
        return RunnerDetection(true, action, obstacle.left, feverShare + obstacle.share)
    }

    private data class Component(val left: Double, val right: Double, val top: Double, val bottom: Double, val share: Double)

    private val NEIGHBOURS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    private fun isObstacle(r: Int, g: Int, b: Int): Boolean {
        val red = r > 100 && r > g * 1.35 && r > b * 1.10
        val magenta = r > 100 && b > 85 && r > g * 1.35 && b > g * 1.25
        return red || magenta
    }

    private fun isFever(r: Int, g: Int, b: Int): Boolean {
        val green = g > 115 && g > r * 1.20 && g > b * .85
        val pink = r > 145 && b > 120 && r > g * 1.25 && b > g * 1.10
        return green || pink
    }

    private fun isPausePurple(r: Int, g: Int, b: Int): Boolean =
        r in 70..205 && b > 125 && b > g * 1.35 && r > g * 1.15

    private inline fun ratio(
        width: Int,
        height: Int,
        x0: Double,
        y0: Double,
        x1: Double,
        y1: Double,
        step: Int,
        argbAt: (Int, Int) -> Int,
        match: (Int, Int, Int) -> Boolean,
    ): Double {
        var hits = 0
        var total = 0
        for (y in (height * y0).toInt() until (height * y1).toInt() step step) {
            for (x in (width * x0).toInt() until (width * x1).toInt() step step) {
                val c = argbAt(x, y)
                if (match(c shr 16 and 255, c shr 8 and 255, c and 255)) hits++
                total++
            }
        }
        return hits.toDouble() / total.coerceAtLeast(1)
    }
}

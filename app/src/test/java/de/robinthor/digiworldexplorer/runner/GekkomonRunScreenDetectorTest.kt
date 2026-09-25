package de.robinthor.digiworldexplorer.runner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GekkomonRunScreenDetectorTest {
    private val width = 360
    private val height = 640

    @Test fun ignoresObstacleColoursWithoutFeverBar() {
        val detection = detect { x, y -> if (x in 180..210 && y in 330..390) 0xffff2030.toInt() else 0xff101820.toInt() }
        assertFalse(detection.active)
    }

    @Test fun ignoresFeverColoursWithoutPauseControl() {
        val detection = detect { x, y ->
            val fx = x.toDouble() / width
            val fy = y.toDouble() / height
            if (fx in .20..0.83 && fy in .15..0.19) 0xff24d96b.toInt() else 0xff101820.toInt()
        }
        assertFalse(detection.active)
    }

    @Test fun lowShortObstacleTriggersJump() {
        val detection = detect(frame(obstacleTop = .57, obstacleBottom = .70))
        assertTrue(detection.active)
        assertEquals(RunnerAction.JUMP, detection.action)
    }

    @Test fun floatingObstacleTriggersSlide() {
        val detection = detect(frame(obstacleTop = .38, obstacleBottom = .54))
        assertTrue(detection.active)
        assertEquals(RunnerAction.SLIDE, detection.action)
    }

    @Test fun distantObstacleWaits() {
        val detection = detect(frame(obstacleLeft = .80, obstacleRight = .88, obstacleTop = .57, obstacleBottom = .70))
        assertTrue(detection.active)
        assertEquals(RunnerAction.NONE, detection.action)
    }

    private fun frame(
        obstacleLeft: Double = .58,
        obstacleRight: Double = .66,
        obstacleTop: Double,
        obstacleBottom: Double,
    ): (Int, Int) -> Int = { x, y ->
        val fx = x.toDouble() / width
        val fy = y.toDouble() / height
        when {
            fx in .765.. .895 && fy in .045.. .125 -> 0xff8f20d8.toInt()
            fx in .20..0.83 && fy in .15..0.19 -> 0xff24d96b.toInt()
            fx in obstacleLeft..obstacleRight && fy in obstacleTop..obstacleBottom -> 0xffff204c.toInt()
            else -> 0xff101820.toInt()
        }
    }

    private fun detect(pixel: (Int, Int) -> Int) = GekkomonRunScreenDetector.detect(width, height, pixel)
}

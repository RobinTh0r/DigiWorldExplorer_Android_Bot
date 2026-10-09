package de.robinthor.digiworldexplorer.detection

import de.robinthor.digiworldexplorer.strategy.ActionKind
import de.robinthor.digiworldexplorer.strategy.MovementPlanner
import de.robinthor.digiworldexplorer.strategy.PlayerSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/**
 * Regression gegen einen echten Geraete-Frame (Samsung SM-G998B, 1080x2400, 20:9).
 * Auf diesem Seitenverhaeltnis fuellt das Spielfeld die Panelbreite fast randlos aus
 * (linke Kante bei ~2,5% der Breite) und die Trennlinien sind mit einem Gradienten
 * von ~20 deutlich weicher als in synthetischen Testbildern.
 */
class RealScreenshotDetectionTest {
    @Test fun rewardStripIsNotTheLastBoardRow() {
        val (w,h,px)=frame("dws_1_5_reward_footer.png")
        val b=requireNotNull(GridDetector.detect(w,h,px)).bounds
        assertTrue("$b",b.top in 340..355 && b.bottom in 785..810)
        val cells=CellClassifier.classify(w,h,px,b,allSprites=false,legacyV4Core=true)
        assertTrue(CalibrationValidator.plausible(cells))
        assertEquals(Cell(2,1),PlayerSelector.select(cells,null,null,emptySet(),legacyV4Core=true)?.key)
        assertEquals(271,HudCounterReader.read(w,h,px,b,updatedActionRow=true).dash)
    }
    @Test fun updatedDwsBlueStacksGreenDashIsSeparateFromBroom() {
        val (width, height, pixels) = frame("dws_1_5_bluestacks.png")
        for (legacy in listOf(true, false)) {
            val detected = requireNotNull(GridDetector.detect(width, height, pixels))
            val b = detected.bounds
            assertTrue("new DWS grid $b", b.left in 75..95 && b.top in 340..355 && b.right in 615..635 && b.bottom in 795..815)
            val dash = requireNotNull(DashButtonLocator.locate(width, height, pixels, b))
            assertTrue("Dash must be inside the lower-left green action, not the broom: $dash",
                dash.first in 395f..455f && dash.second in 1080f..1180f)
            val hud = HudCounterReader.read(width, height, pixels, b, updatedActionRow = true)
            assertEquals("green stock must not become broom stock", 271, hud.dash)
            assertTrue("green stock box must be above broom row", requireNotNull(hud.dashBox).bottom < 1150)
            val cells = CellClassifier.classify(width, height, pixels, b, allSprites = !legacy, legacyV4Core = legacy)
            assertTrue("new DWS board should be plausible for legacy=$legacy", CalibrationValidator.plausible(cells))
            assertEquals("player location on updated DWS, legacy=$legacy", Cell(4, 0), PlayerSelector.select(cells, null, null, emptySet(), legacyV4Core = legacy)?.key)
        }
    }
    @Test fun isolatedGreenDashIsFoundButIsolatedBroomIsNot() {
        val (width, height, pixels) = frame("dws_1_5_bluestacks.png")
        val bounds = requireNotNull(GridDetector.detect(width, height, pixels)).bounds
        fun withoutButton(xRange: IntRange, yRange: IntRange): IntArray = pixels.copyOf().also { copy ->
            for (y in yRange) for (x in xRange) copy[y * width + x] = 0xff68707a.toInt()
        }

        // Hide the green Dash action: the upper-right broom must never be tapped as Dash.
        val rightOnly = withoutButton(355..490, 1040..1195)
        assertEquals(null, DashButtonLocator.locate(width, height, rightOnly, bounds))

        // With the broom hidden, the lower-left green Dash is still safe to locate.
        val leftOnly = withoutButton(485..620, 970..1120)
        val dash = requireNotNull(DashButtonLocator.locate(width, height, leftOnly, bounds))
        assertTrue("isolated green Dash $dash", dash.first in 395f..455f && dash.second in 1080f..1180f)
    }
    @Test fun highlightedTrainingPointWinsOverDistantEnergyWithoutInventingAWall() {
        val (width, height, pixels) = frame("dws_training_points_highlight.jpg")
        val bounds = requireNotNull(GridDetector.detect(width, height, pixels)).bounds
        val dash = requireNotNull(DashButtonLocator.locate(width, height, pixels, bounds))
        assertTrue("old UI single green Dash $dash", dash.first in 280f..345f && dash.second in 860f..970f)
        val cells = CellClassifier.classify(width, height, pixels, bounds)
        assertTrue(CalibrationValidator.plausible(cells))
        assertTrue("training points must be visible", cells.getValue(Cell(0, 2)).pink > .06)
        assertTrue("cyan free tile is not a pyramid", !cells.getValue(Cell(1, 1)).obstacle())
        assertEquals(Cell(0, 2), MovementPlanner.choose(Cell(0, 1), cells, emptyList())?.target)
    }
    @Test fun detectsScaledMumuGridWithWeakHorizontalLines() {
        val (width, height, pixels) = frame("mumu_dws_dark_city.jpg")
        for (scale in 1..2) {
            val w = width * scale; val h = height * scale
            val scaled = if (scale == 1) pixels else IntArray(w * h) { i ->
                pixels[(i / w / scale) * width + (i % w / scale)]
            }
            val detection = requireNotNull(GridDetector.detect(w, h, scaled))
            assertTrue("MuMu confidence ${detection.confidence}", detection.confidence >= .55)
            assertTrue("MuMu left ${detection.bounds.left}", detection.bounds.left / scale in 45..60)
            assertTrue("MuMu top ${detection.bounds.top}", detection.bounds.top / scale in 260..275)
            assertTrue("MuMu right ${detection.bounds.right}", detection.bounds.right / scale in 460..475)
            assertTrue("MuMu bottom ${detection.bounds.bottom}", detection.bounds.bottom / scale in 640..655)
            val cells = CellClassifier.classify(w, h, scaled, detection.bounds)
            assertTrue("MuMu board must pass calibration", CalibrationValidator.plausible(cells))
            assertEquals(Cell(2, 1), cells.maxByOrNull { it.value.player }?.key)
        }
    }

    private fun frame(name: String): Triple<Int, Int, IntArray> {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(name)) { "missing $name" }
        val image = stream.use { ImageIO.read(it) }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return Triple(image.width, image.height, pixels)
    }

    @Test
    fun detectsGridOnRealDeviceFrame() {
        val (width, height, pixels) = frame("samsung_1080x2400.png")
        val detection = GridDetector.detect(width, height, pixels)
        assertNotNull("kein Raster auf echtem 1080x2400 Frame erkannt", detection)
        detection!!

        assertTrue("Konfidenz ${detection.confidence} unter der Freigabeschwelle", detection.confidence >= .82)
        val b = detection.bounds
        assertTrue("linke Kante ${b.left}", b.left in 15..40)
        assertTrue("obere Kante ${b.top}", b.top in 835..860)
        assertTrue("Zellbreite ${(b.right - b.left) / 5.0}", (b.right - b.left) / 5.0 in 190.0..208.0)
        assertTrue("Zellhoehe ${(b.bottom - b.top) / 5.0}", (b.bottom - b.top) / 5.0 in 155.0..172.0)
    }

    @Test
    fun classifiesRealDeviceFrame() {
        val (width, height, pixels) = frame("samsung_1080x2400.png")
        val detection = requireNotNull(GridDetector.detect(width, height, pixels))
        val cells = CellClassifier.classify(width, height, pixels, detection.bounds)

        assertTrue("Frame muss als Spielbild gelten", CalibrationValidator.plausible(cells))

        val player = PlayerSelector.select(cells, null, null, emptySet())
        assertEquals(Cell(3, 1), player?.key)

        assertEquals(
            listOf(Cell(1, 3)),
            cells.filter { it.value.item > .06 }.keys.sortedWith(compareBy({ it.row }, { it.col })),
        )
        assertEquals(
            listOf(Cell(0, 3), Cell(1, 0), Cell(4, 0)),
            cells.filter { it.value.obstacle() }.keys.sortedWith(compareBy({ it.row }, { it.col })),
        )
    }

    @Test
    fun plansMoveTowardsItemOnRealDeviceFrame() {
        val (width, height, pixels) = frame("samsung_1080x2400.png")
        val detection = requireNotNull(GridDetector.detect(width, height, pixels))
        val cells = CellClassifier.classify(width, height, pixels, detection.bounds)
        val preview = PreviewClassifier.classify(width, height, pixels, detection.bounds)
        val player = requireNotNull(PlayerSelector.select(cells, null, null, emptySet())).key

        val action = MovementPlanner.choose(player, cells, listOf(player), false, preview, emptySet())
        assertEquals(ActionKind.MOVE, action?.kind)
        // Zum Item auf (1,3) fuehren mehrere gleich lange Wege; entscheidend ist, dass der Zug den
        // Abstand verringert und nicht, welche der gleichwertigen Richtungen gewaehlt wird.
        val item = Cell(1, 3)
        val before = Math.abs(player.row - item.row) + Math.abs(player.col - item.col)
        val target = requireNotNull(action).target
        val after = Math.abs(target.row - item.row) + Math.abs(target.col - item.col)
        assertTrue("Zug $target vergroessert den Abstand zum Item", after < before)
    }
}

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

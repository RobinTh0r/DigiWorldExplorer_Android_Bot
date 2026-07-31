package de.robinthor.digiworldexplorer.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/**
 * Echter Geraete-Frame, in dem das Spiel mehrere Zellen hellblau als erreichbar markiert und die
 * Figur auf einer dieser Markierungen steht. Gemessen wurden hier Pyramiden-Anteile von 0.81-0.89
 * auf den Hindernisfeldern und ein Schattenwert von 0.128 auf der Figur - der Frame ist also
 * eindeutig, obwohl das Geraet in derselben Sekunde voellig falsche Werte gemeldet hat. Die
 * Fehlmessung stammte aus einem Zwischenbild der Scroll-Animation, nicht aus dem Klassifikator.
 */
class HighlightFrameDetectionTest {

    private fun frame(name: String): Triple<Int, Int, IntArray> {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(name)) { "missing $name" }
        val image = stream.use { ImageIO.read(it) }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return Triple(image.width, image.height, pixels)
    }

    @Test
    fun classifiesHighlightedBoard() {
        val (width, height, pixels) = frame("samsung_highlight.png")
        val bounds = requireNotNull(GridDetector.detect(width, height, pixels)).bounds
        val cells = CellClassifier.classify(width, height, pixels, bounds)

        val player = requireNotNull(cells.maxByOrNull { it.value.player })
        assertEquals("Figur steht auf der hellblau markierten Zelle", Cell(1, 1), player.key)
        assertTrue("Figurenscore ${player.value.player} zu schwach", player.value.player >= .10)

        val items = cells.filter { it.key != player.key && it.value.item > .06 }.keys
        assertTrue("Markierungen faelschlich als Item erkannt: $items", items.isEmpty())

        val obstacles = cells.filter { it.value.obstacle() }.keys.sortedWith(compareBy({ it.row }, { it.col }))
        assertEquals(
            listOf(Cell(1, 4), Cell(2, 0), Cell(2, 1), Cell(3, 1), Cell(3, 2), Cell(4, 0), Cell(4, 1)),
            obstacles,
        )
    }
}

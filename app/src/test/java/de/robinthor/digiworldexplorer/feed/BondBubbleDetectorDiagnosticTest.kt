package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import javax.imageio.ImageIO

class BondBubbleDetectorDiagnosticTest {
    private fun frame(name: String): PixelFrame {
        val image = ImageIO.read(requireNotNull(javaClass.getResource("/$name")))
        return PixelFrame(image.width, image.height) { x, y -> image.getRGB(x, y) }
    }

    @Test fun `oneplus bubble target is the bubble and never the figure underneath`() {
        val bubble = BondBubbleDetector.detect(frame("oneplus_bond_home_bubble.jpg"))
        assertNotNull(bubble)
        assertEquals(bubble, BondBubbleDetector.tapTarget(bubble!!))
    }

    @Test fun `partner popup cannot authorize a collection tap`() {
        assertNull(BondBubbleDetector.detect(frame("oneplus_bond_partner_popup.jpg")))
    }
}

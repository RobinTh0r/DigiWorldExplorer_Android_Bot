package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.PixelFrame
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Test

class FarmResourceReadersTest {
    private fun frame(name: String): PixelFrame {
        val image = ImageIO.read(requireNotNull(javaClass.getResource("/$name")))
        return PixelFrame(image.width, image.height) { x, y -> image.getRGB(x, y) }
    }

    @Test fun waterCounterDoesNotIncludeSeedRow() {
        assertEquals(16, FarmResourceReaders.wateringCans(frame("farm_water_16.png")))
        assertEquals(0, FarmResourceReaders.wateringCans(frame("farm_water_0.png")))
    }

    @Test fun emptyPremiumSlotStaysZero() {
        assertEquals(listOf(13, 1, 0), FarmResourceReaders.dialogSeeds(frame("farm_seeds_13_1_0.png")))
    }
}

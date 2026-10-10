package de.robinthor.digiworldexplorer.detection

import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO

class LiveDwsCalibrationTest {
    @Test fun native2424BotamonCannotBeConfusedWithEnergyOnTheRowBelow() {
        val image=ImageIO.read(javaClass.getResource("/live_dws_1080x2424.png"))
        val pixels=image.getRGB(0,0,image.width,image.height,null,0,image.width)
        val grid=GridDetector.detect(image.width,image.height,pixels)!!
        val cells=CellClassifier.classify(image.width,image.height,pixels,grid.bounds,allSprites=false,legacyV4Core=true)
        assertEquals("grid=$grid cells=$cells",Cell(3,1),cells.maxByOrNull {it.value.player}!!.key)
    }
    @Test fun nativeTallDisplayMeasuresTheResourcePlatesInsteadOfTruncatingDigits() {
        val image=ImageIO.read(javaClass.getResource("/live_dws_stock_1220x2712.png"))
        val pixels=image.getRGB(0,0,image.width,image.height,null,0,image.width)
        val grid=GridDetector.detect(image.width,image.height,pixels)!!
        val layout=HudCounterReader.detectActionRowLayout(image.width,image.height,pixels,grid.bounds)
        val hud=HudCounterReader.read(image.width,image.height,pixels,grid.bounds,layout)
        assertTrue(hud.updatedActionRow)
        // Leading 1 proves >=100, not an invented exact 179 when 9 is unknown.
        assertEquals(100,hud.dash)
        assertTrue(hud.dashMinimumOnly)
        assertNotNull(hud.dashBox)
        assertTrue(hud.dashBox!!.top>image.height*.83)
        assertNotNull(DashButtonLocator.locate(image.width,image.height,pixels,grid.bounds))
    }
}

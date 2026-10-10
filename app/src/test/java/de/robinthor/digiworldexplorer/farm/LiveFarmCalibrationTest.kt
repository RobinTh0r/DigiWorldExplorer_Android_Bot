package de.robinthor.digiworldexplorer.farm

import de.robinthor.digiworldexplorer.vision.*
import org.junit.Assert.*
import org.junit.Test
import javax.imageio.ImageIO

class LiveFarmCalibrationTest {
    @Test fun actualDwsCloseSquareUsesObservedGeometryToo() {
        val target=FarmHarvestDetector.closeTarget(frame("live_dws_1220x2712"))
        assertNotNull(target)
        assertEquals(.91,target!!.x,.025)
        assertEquals(.94,target.y,.025)
    }
    @Test fun actualCloseSquareIsOnTheFarRightNotTheHistorical835PercentPoint() {
        val target=FarmHarvestDetector.closeTarget(frame("live_farm_1220x2712"))
        assertNotNull(target)
        assertEquals(.91,target!!.x,.025)
        assertEquals(.968,target.y,.02)
    }
    @Test fun actualTimerPlatesProveGrowingAfterWaterBubblesDisappear() {
        val field=FarmHarvestDetector.detect(frame("live_farm_no_water_1220x2712"))
        assertTrue(field.toString(),field.field)
        assertEquals(listOf(PlotState.GROWING,PlotState.LOCKED,PlotState.GROWING,PlotState.GROWING,PlotState.GROWING,PlotState.GROWING),field.states)
        assertTrue(field.waterablePlots.toString(),field.waterablePlots.isEmpty())
    }
    @Test fun nativeGrowingWaterDialogHasItsOwnButton() {
        val dialog=FarmDialogDetector.detect(frame("live_water_dialog_1220x2712"),6,trustedFarmFlow=true)
        assertEquals(dialog.toString(),FarmView.WATER,dialog.view)
        assertEquals(.5,dialog.selectButton!!.x,.02)
        assertEquals(.663,dialog.selectButton.y,.02)
    }
    @Test fun allFivePlantedPlotsAreGrowingAndLockedPlotRemainsLocked() {
        val field=FarmHarvestDetector.detect(frame("live_farm_all_grown_1220x2712"))
        assertTrue(field.toString(),field.field)
        assertEquals(listOf(PlotState.GROWING,PlotState.LOCKED,PlotState.GROWING,PlotState.GROWING,PlotState.GROWING,PlotState.GROWING),field.states)
        assertEquals(setOf(0,2,3,4,5),field.waterablePlots)
    }
    @Test fun joinedGrowingSoilStillLocatesLastEmptyPlotAndFourWaterBubbles() {
        val field=FarmHarvestDetector.detect(frame("live_farm_growing_1220x2712"))
        assertTrue(field.toString(),field.field)
        assertEquals(listOf(PlotState.GROWING,PlotState.LOCKED,PlotState.GROWING,PlotState.GROWING,PlotState.GROWING,PlotState.EMPTY),field.states)
        assertEquals(.727,field.actionTargets[5]!!.x,.025)
        assertEquals(.746,field.actionTargets[5]!!.y,.025)
        assertEquals(setOf(0,2,3,4),field.waterablePlots)
    }
    private fun frame(name:String):PixelFrame {
        val image=ImageIO.read(javaClass.getResource("/$name.png"))
        return PixelFrame(image.width,image.height,image::getRGB)
    }
    @Test fun nativeShovelsAreEmptyAndMustNotBeHarvested() {
        val field=FarmHarvestDetector.detect(frame("live_farm_1220x2712"))
        assertTrue(field.toString(),field.field)
        assertEquals(listOf(PlotState.EMPTY,PlotState.LOCKED,PlotState.EMPTY,PlotState.RIPE,PlotState.RIPE,PlotState.RIPE),field.states)
    }
    @Test fun nativeSeedDialogUsesTheWideVisibleChooseButtonNotHudWateringIcon() {
        val dialog=FarmDialogDetector.detect(frame("live_seeds_1220x2712"),6,trustedFarmFlow=true)
        assertEquals(dialog.toString(),FarmView.SEEDS,dialog.view)
        // Counters only authorize availability, not exact stock accounting (2/3 font ambiguity).
        assertTrue(dialog.seedCounts.toString(),dialog.seedCounts.all { (it ?: 0)>0 })
        assertEquals(9,dialog.seedCounts[2])
        assertEquals(0,dialog.selectedSlot)
        assertEquals(.50,dialog.selectButton!!.x,.015)
        assertEquals(.586,dialog.selectButton.y,.02)
    }
}

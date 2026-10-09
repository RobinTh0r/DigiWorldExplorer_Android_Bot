package de.robinthor.digiworldexplorer.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/**
 * Echter Geraeteframe mit Pfoten 197, Krallen 1 und Dash 2. Die Ziffern sind Outline-Glyphen,
 * die Vorlagen stammen aus genau diesem Bild - der Test sichert damit vor allem ab, dass Bandlage,
 * Segmentierung und Normierung stabil bleiben, wenn an der Erkennung geschraubt wird.
 */
class HudCounterReaderTest {
    @Test fun readsStockAfterFirstActualDash() {
        val (w,h,px)=frame("dws_1_5_dash_270.png")
        val hud=HudCounterReader.read(w,h,px,GridBounds(83,349,621,801),updatedActionRow=true)
        assertEquals(270,hud.dash)
        assertEquals(false,hud.dashMinimumOnly)
    }
    @Test fun provenPrefixAllowsOnlyAnExplicitConservativeMinimum() {
        val (w,h,px)=frame("dws_1_5_bluestacks.png")
        val b=requireNotNull(GridDetector.detect(w,h,px)).bounds
        for(y in 1095..1140) for(x in 184..230) px[y*w+x]=0xff879bb7.toInt()
        for(start in listOf(185,201)) for(y in 1106..1126) for(x in start until start+14)
            if(kotlin.math.abs((x-start)*20/13-(y-1106))<=1 || kotlin.math.abs((13-(x-start))*20/13-(y-1106))<=1) px[y*w+x]=0xffffffff.toInt()
        val hud=HudCounterReader.read(w,h,px,b,updatedActionRow=true)
        assertEquals(200,hud.dash)
        assertTrue(hud.dashMinimumOnly)
        // Losing the recognized first digit removes proof of positive stock entirely.
        for(y in 1095..1140) for(x in 160..184) px[y*w+x]=0xff879bb7.toInt()
        assertEquals(null,HudCounterReader.read(w,h,px,b,updatedActionRow=true).dash)
    }

    private fun frame(name: String = "samsung_hud.png"): Triple<Int, Int, IntArray> {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(name))
        val image = stream.use { ImageIO.read(it) }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return Triple(image.width, image.height, pixels)
    }

    private val bounds = GridBounds(27, 847, 1021, 1664)

    @Test
    fun readsClawsAndDash() {
        val (w, h, px) = frame()
        val hud = HudCounterReader.read(w, h, px, bounds)
        assertEquals(1, hud.claws)
        assertEquals(2, hud.dash)
    }

    /**
     * Zweiter Geraeteframe, aufgenommen nachdem eine Kralle nachgewachsen war. Hier liegt das
     * Overlay bereits mit im Bild - der Kasten um die Zahl darf sie also nicht verdecken.
     */
    @Test
    fun readsRegeneratedClawCount() {
        val (w, h, px) = frame("samsung_hud_three.png")
        val hud = HudCounterReader.read(w, h, px, bounds)
        assertEquals(3, hud.claws)
        assertEquals(2, hud.dash)
        assertTrue("keine unbekannten Formen erwartet: ${hud.unknown}", hud.unknown.isEmpty())
    }

    @Test
    fun reportsBoxesForOverlay() {
        val (w, h, px) = frame()
        val hud = HudCounterReader.read(w, h, px, bounds)
        val claws = assertNotNull(hud.clawsBox).let { hud.clawsBox!! }
        val dash = assertNotNull(hud.dashBox).let { hud.dashBox!! }
        // Gemessen: Krallenzahl y=2142..2171, Dashzahl y=2227..2257, beide ab x=289.
        assertTrue("Krallenkasten $claws", claws.top in 2135..2150 && claws.bottom in 2165..2180)
        assertTrue("Dashkasten $dash", dash.top in 2220..2235 && dash.bottom in 2250..2265)
        assertTrue("Kaesten duerfen sich nicht ueberlappen", claws.bottom < dash.top)
    }

    @Test
    fun rejectsUnknownGlyphsInsteadOfGuessing() {
        val (w, h, px) = frame()
        // Das Pfotenband traegt eine dreistellige Zahl, deren Ziffern sich beruehren. Genau so ein
        // Fall darf keine erfundene Zahl liefern.
        val paws = HudCounterReader.readBand(w, h, px, bounds, HudCounterReader.PAWS_BAND)
        assertNotNull(paws)
        assertEquals(null, paws!!.first)
    }

    @Test
    fun S22DisabledDashIsUnknownRatherThanInventedAsPositive() {
        val (w, h, px) = frame("s22_world_search_dash_zero.jpg")
        val grid = requireNotNull(GridDetector.detect(w, h, px))
        assertEquals(null, HudCounterReader.read(w, h, px, grid.bounds).dash)
    }

    @Test
    fun updatedHudUsesGreenThirdRowAndDoesNotReadBroomAsDash() {
        val (w, h, px) = frame("dws_1_5_bluestacks.png")
        val bounds = requireNotNull(GridDetector.detect(w, h, px)).bounds
        assertEquals(true, HudCounterReader.hasUpdatedActionRow(w, h, px, bounds))

        // The fourth row really contains a readable 2, but it belongs to the broom resource.
        assertEquals(2, HudCounterReader.readBand(w, h, px, bounds, .82, .13, .44)?.first)
        val hud = HudCounterReader.read(w, h, px, bounds, updatedActionRow = true)
        val greenBox = assertNotNull(hud.dashBox).let { hud.dashBox!! }
        assertTrue("green Dash count must be above the broom row: $greenBox", greenBox.top in 1100..1130 && greenBox.bottom < 1150)
        // Touching outline digits must be separated; the broom's 2 is not the green stock.
        assertEquals(271, hud.dash)
        // If the fourth-row icon is obscured, the old layout path must not invent broom stock.
        assertEquals(null, HudCounterReader.read(w, h, px, bounds, updatedActionRow = false).dash)
    }

    @Test
    fun oldHudKeepsItsThirdRowDashCountWithoutFourthRowEvidence() {
        val (w, h, px) = frame()
        assertEquals(false, HudCounterReader.hasUpdatedActionRow(w, h, px, bounds))
        assertEquals(2, HudCounterReader.read(w, h, px, bounds).dash)
    }

    @Test fun missingGreenStockNeverUsesPositiveBroomStock() {
        val (w,h,px)=frame("dws_1_5_bluestacks.png")
        val b=requireNotNull(GridDetector.detect(w,h,px)).bounds
        for(y in 1095..1140) for(x in 160..260) px[y*w+x]=0xff879bb7.toInt()
        assertEquals(2,HudCounterReader.readBand(w,h,px,b,.82,.13,.44)?.first)
        assertEquals(null,HudCounterReader.read(w,h,px,b,updatedActionRow=true).dash)
    }

    @Test fun updatedZeroOutlineIsNotInventedAsPositiveStock() {
        val (w,h,px)=frame("dws_1_5_bluestacks.png")
        val b=requireNotNull(GridDetector.detect(w,h,px)).bounds
        for(y in 1095..1140) for(x in 160..260) px[y*w+x]=0xff879bb7.toInt()
        // A closed 0 contour in the actual number band: unsupported is safe, positive is not.
        for(y in 1106..1126) for(x in 170..183)
            if(y==1106||y==1126||x==170||x==183) px[y*w+x]=0xffffffff.toInt()
        assertTrue(HudCounterReader.read(w,h,px,b,updatedActionRow=true).dash.let { it==null||it==0 })
    }
}

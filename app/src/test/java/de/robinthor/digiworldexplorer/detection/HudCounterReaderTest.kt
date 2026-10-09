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
        // The visible 271 merges into one unsupported glyph; unknown is safer than reporting broom 2.
        assertEquals(null, hud.dash)
        assertTrue("unrecognized green digits should be retained for calibration", hud.unknown.isNotEmpty())
        // If the fourth-row icon is obscured, the old layout path must not invent broom stock.
        assertEquals(null, HudCounterReader.read(w, h, px, bounds, updatedActionRow = false).dash)
    }

    @Test
    fun oldHudKeepsItsThirdRowDashCountWithoutFourthRowEvidence() {
        val (w, h, px) = frame()
        assertEquals(false, HudCounterReader.hasUpdatedActionRow(w, h, px, bounds))
        assertEquals(2, HudCounterReader.read(w, h, px, bounds).dash)
    }
}

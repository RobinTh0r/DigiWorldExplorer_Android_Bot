package de.robinthor.digiworldexplorer.detection

import kotlin.math.roundToInt

/** Bildschirmrechteck einer HUD-Zahl, damit das Overlay sie einkasten kann. */
data class HudBox(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Gelesene Vorratszahlen. `null` heisst "nicht sicher erkannt" und muss vorsichtig behandelt werden. */
data class HudCounters(
    val claws: Int? = null,
    val clawsBox: HudBox? = null,
    val dash: Int? = null,
    val dashBox: HudBox? = null,
    /** Normalformen der Ziffern, die zu keiner Vorlage passten - Rohmaterial fuer neue Vorlagen. */
    val unknown: List<String> = emptyList(),
    val updatedActionRow: Boolean = false,
    /** True when only a proven multi-digit prefix supplies a conservative minimum stock. */
    val dashMinimumOnly: Boolean = false,
)

enum class HudActionRowLayout { LEGACY, UPDATED, UNKNOWN }

/**
 * Liest die Vorratszahlen fuer Angriffskrallen und Dash unter dem Spielfeld.
 *
 * Die Ziffern sind Outline-Glyphen: nur die Kontur ist weiss, die Fuellung liegt farblich darunter.
 * Das Spiel rendert sie immer pixelgleich an derselben Stelle, deshalb wird die Kontur nicht
 * interpretiert, sondern auf ein festes Raster normiert und direkt mit aufgezeichneten Vorlagen
 * verglichen. Ein Formklassifikator waere bei dieser sehr fetten, leicht kursiven Schrift deutlich
 * unzuverlaessiger - auf 9x6 normiert laufen etwa Eins und Zwei fast zu denselben Bloecken zusammen.
 *
 * Alle Fenster sind relativ zum kalibrierten Raster angegeben. Gemessen auf dem Geraet
 * (1080x2400, Raster 27/847/1021/1664): Krallenzahl auf y=2142..2170, Dashzahl auf y=2227..2256,
 * beide ab x=290; links davon liegen nur das Symbol und der Nachwachs-Timer.
 */
object HudCounterReader {
    private const val CLAWS_CENTER = .602
    private const val DASH_CENTER = .706
    private const val BAND_HALF = .032
    private const val X_FROM = .26
    internal const val X_TO = .42
    private const val WHITE = 225

    /** Hoechstens so viele leere Spalten gelten noch als Teil derselben Ziffer. */
    private const val COLUMN_GAP = 0

    /** Eine Ziffer muss mindestens so hoch sein, sonst ist es Bildrauschen. */
    private const val MIN_HEIGHT = 12

    /** Ab so viel Kontur gilt eine Rasterzelle der Normalform als gesetzt. */
    private const val CELL_ON = 35

    internal const val ROWS = 16
    internal const val COLS = 12

    /** Groesster noch akzeptierter Abstand zur besten Vorlage (von [ROWS]*[COLS] Feldern). */
    private const val MAX_DISTANCE = 26

    /** So viel besser muss die beste Vorlage gegenueber der zweitbesten sein. */
    private const val MIN_MARGIN = 8

    /**
     * Aufgezeichnete Konturformen. Belegt sind nur Ziffern, die auf dem Geraet tatsaechlich
     * beobachtet wurden. Alles andere liefert bewusst `null` statt einer Vermutung - die
     * Bewegungsplanung behandelt "unbekannt" ohnehin so vorsichtig wie "fast leer".
     */
    private const val FIVE =
        "001111111110" +   // ..##########
        "011111111111" +   // .###########
        "010000000001" +   // .#.........#
        "010000000001" +   // .#.........#
        "010000111111" +   // .#....######
        "010001111100" +   // .#...#####..
        "010000111110" +   // .#....#####.
        "010000000010" +   // .#........#.
        "010000000001" +   // .#.........#
        "011111100001" +   // .######....#
        "011100110001" +   // .###..##...#
        "010111110001" +   // .#.#####...#
        "110000000001" +   // ##.........#
        "110000000011" +   // ##........##
        "011000001110" +   // .##.....###.
        "001111111000"     // ..#######...

    private val TEMPLATES: List<Pair<Int, String>> = listOf(
        1 to ONE,
        1 to ONE_UPDATED,
        2 to TWO,
        2 to TWO_UPDATED,
        2 to TWO_GREEN_UPDATED,
        2 to TWO_SHORT_UPDATED,
        3 to THREE,
        5 to FIVE,
        7 to SEVEN_UPDATED,
        7 to SEVEN_SHORT_UPDATED,
        0 to ZERO_UPDATED,
    )

    /** Distinguishes the fourth-row broom HUD from the old three-row HUD without using buttons. */
    fun detectActionRowLayout(width: Int, height: Int, argb: IntArray, bounds: GridBounds): HudActionRowLayout {
        if (width <= 0 || height <= 0 || argb.size.toLong() < width.toLong() * height)
            return HudActionRowLayout.UNKNOWN
        val gridWidth = (bounds.right - bounds.left).toDouble()
        val gridHeight = (bounds.bottom - bounds.top).toDouble()
        if (gridWidth <= 0 || gridHeight <= 0) return HudActionRowLayout.UNKNOWN

        // The 1.5 fourth resource slot has a flat slate fill. Nine interior points avoid its
        // broom icon and number, so a temporarily obscured icon does not imply the old layout.
        var slate = 0
        for (row in doubleArrayOf(.78, .80, .82)) for (column in doubleArrayOf(.22, .28, .35)) {
            val x = (bounds.left + column * gridWidth).toInt().coerceIn(0, width - 1)
            val y = (bounds.bottom + row * gridHeight).toInt().coerceIn(0, height - 1)
            val pixel = argb[y * width + x]
            val red = pixel shr 16 and 255
            val green = pixel shr 8 and 255
            val blue = pixel and 255
            if (red in 105..170 && green in 125..195 && blue in 150..220 &&
                green - red in 8..35 && blue - green in 10..40) slate++
        }
        if (slate >= 7) return HudActionRowLayout.UPDATED

        // The orange broom is a second positive marker for the updated fourth resource row.
        val x0 = (bounds.left + .02 * gridWidth).toInt().coerceIn(0, width - 1)
        val x1 = (bounds.left + .15 * gridWidth).toInt().coerceIn(x0 + 1, width)
        val y0 = (bounds.bottom + .74 * gridHeight).toInt().coerceIn(0, height - 1)
        val y1 = (bounds.bottom + .90 * gridHeight).toInt().coerceIn(y0 + 1, height)
        val required = maxOf(12, (x1 - x0) * (y1 - y0) / 150)
        var orange = 0
        for (y in y0 until y1) for (x in x0 until x1) {
            val pixel = argb[y * width + x]
            val red = pixel shr 16 and 255
            val green = pixel shr 8 and 255
            val blue = pixel and 255
            if (red > 170 && green in 71..214 && blue < 140 &&
                red > green + 35 && green > blue + 25 && ++orange >= required)
                return HudActionRowLayout.UPDATED
        }

        // The old green Dash icon sits farther right than the 1.5 icon. Absence of a broom alone
        // cannot confirm legacy: the icon or whole fourth row may be covered or desaturated.
        val oldX0 = (bounds.left + .13 * gridWidth).toInt().coerceIn(0, width - 1)
        val oldX1 = (bounds.left + .23 * gridWidth).toInt().coerceIn(oldX0 + 1, width)
        val oldY0 = (bounds.bottom + .65 * gridHeight).toInt().coerceIn(0, height - 1)
        val oldY1 = (bounds.bottom + .76 * gridHeight).toInt().coerceIn(oldY0 + 1, height)
        val oldRequired = maxOf(12, (oldX1 - oldX0) * (oldY1 - oldY0) / 50)
        var oldGreen = 0
        for (y in oldY0 until oldY1) for (x in oldX0 until oldX1) {
            val pixel = argb[y * width + x]
            val red = pixel shr 16 and 255
            val green = pixel shr 8 and 255
            val blue = pixel and 255
            if (green > 150 && green > red + 30 && green > blue + 20 &&
                ++oldGreen >= oldRequired) return HudActionRowLayout.LEGACY
        }
        return HudActionRowLayout.UNKNOWN
    }

    fun hasUpdatedActionRow(width: Int, height: Int, argb: IntArray, bounds: GridBounds): Boolean =
        detectActionRowLayout(width, height, argb, bounds) == HudActionRowLayout.UPDATED

    fun read(width: Int, height: Int, argb: IntArray, bounds: GridBounds, updatedActionRow: Boolean = false): HudCounters =
        read(width, height, argb, bounds, if (updatedActionRow) HudActionRowLayout.UPDATED else HudActionRowLayout.LEGACY)

    fun read(width: Int, height: Int, argb: IntArray, bounds: GridBounds, layout: HudActionRowLayout): HudCounters {
        if (layout == HudActionRowLayout.UNKNOWN) return HudCounters()
        val updatedActionRow = layout == HudActionRowLayout.UPDATED
        val clawsCenter = if (updatedActionRow) .57 else CLAWS_CENTER
        // The fourth row at .82 contains broom stock, not Dash. Dash stays in the green third row.
        val dashCenter = if (updatedActionRow) .68 else DASH_CENTER
        val xFrom = if (updatedActionRow) .13 else X_FROM
        val xTo = if (updatedActionRow) .44 else X_TO
        val claws = readBand(width, height, argb, bounds, clawsCenter, xFrom, xTo)
        val dash = readBand(width, height, argb, bounds, dashCenter, xFrom, xTo)
        val minimum = if(updatedActionRow && dash?.first == null)
            positivePrefixMinimum(glyphs(width,height,argb,bounds,dashCenter,xFrom,xTo)) else null
        val unknown = mutableListOf<String>()
        if (claws?.first == null) unknown += glyphs(width, height, argb, bounds, clawsCenter, xFrom, xTo).map { it.bits }
        if (dash?.first == null) unknown += glyphs(width, height, argb, bounds, dashCenter, xFrom, xTo).map { it.bits }
        return HudCounters(claws?.first, claws?.second, dash?.first ?: minimum, dash?.second, unknown, updatedActionRow, minimum!=null)
    }

    /** A confidently read leading 2 in a three-digit stock proves at least 200, even
     * when 6/9 have not yet been calibrated. A zero/unknown/single glyph proves nothing. */
    private fun positivePrefixMinimum(glyphs:List<Glyph>):Int? {
        if(glyphs.size !in 2..5 || glyphs.any {
            val h=it.box.bottom-it.box.top; val w=it.box.right-it.box.left
            h<MIN_HEIGHT || w<h*.30 || w>h*.95 || it.bits.count { bit -> bit=='1' }<20
        }) return null
        val first=classify(glyphs.first().bits) ?: return null
        if(first==0) return null
        return first * (1 until glyphs.size).fold(1) { n,_ -> n*10 }
    }

    /**
     * Liest die Zahl eines Bandes. Der Kasten wird auch dann geliefert, wenn die Ziffer unlesbar
     * bleibt - sichtbar ist sie ja trotzdem, und das Overlay soll das ehrlich anzeigen.
     */
    internal fun readBand(
        width: Int,
        height: Int,
        argb: IntArray,
        bounds: GridBounds,
        center: Double,
        xFrom: Double = X_FROM,
        xTo: Double = X_TO,
    ): Pair<Int?, HudBox>? {
        val glyphs = glyphs(width, height, argb, bounds, center, xFrom, xTo)
        if (glyphs.isEmpty()) return null
        var value: Int? = 0
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        for (glyph in glyphs) {
            left = minOf(left, glyph.box.left); right = maxOf(right, glyph.box.right)
            top = minOf(top, glyph.box.top); bottom = maxOf(bottom, glyph.box.bottom)
            val digit = classify(glyph.bits)
            value = if (digit == null || value == null) null else value * 10 + digit
        }
        return value to HudBox(left, top, right, bottom)
    }

    internal data class Glyph(val bits: String, val box: HudBox)

    internal fun glyphs(
        width: Int,
        height: Int,
        argb: IntArray,
        bounds: GridBounds,
        center: Double,
        xFrom: Double = X_FROM,
        xTo: Double = X_TO,
    ): List<Glyph> {
        val gridHeight = (bounds.bottom - bounds.top).toDouble()
        val gridWidth = (bounds.right - bounds.left).toDouble()
        // Updated compact-grid fits vary by a few pixels. Keep the full glyph baseline;
        // truncating its bottom changed the apparent aspect and split 71 into three pieces.
        val bandHalf = if (xFrom < .20) .042 else BAND_HALF
        val y0 = (bounds.bottom + (center - bandHalf) * gridHeight).toInt().coerceIn(0, height - 1)
        val y1 = (bounds.bottom + (center + bandHalf) * gridHeight).toInt().coerceIn(y0 + 1, height)
        val x0 = (bounds.left + xFrom * gridWidth).toInt().coerceIn(0, width - 1)
        val x1 = (bounds.left + xTo * gridWidth).toInt().coerceIn(x0 + 1, width)
        val w = x1 - x0
        val h = y1 - y0

        val ink = BooleanArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = argb[(y0 + y) * width + (x0 + x)]
                if ((p shr 16 and 255) > WHITE && (p shr 8 and 255) > WHITE && (p and 255) > WHITE) {
                    ink[y * w + x] = true
                }
            }
        }

        val groups = columnGroups(ink, w, h).flatMap { (left, right) ->
            val rows = (0 until h).filter { y -> (left..right).any { x -> ink[y*w+x] } }
            val inkHeight = if (rows.isEmpty()) 0 else rows.last()-rows.first()+1
            val span = right-left+1
            // Current outlined digits can touch at their bottom strokes (notably 7+1).
            // A single digit is narrower than its height; split only genuinely wide groups.
            if (inkHeight >= MIN_HEIGHT && span > inkHeight * 1.05) {
                val count = (span / (inkHeight * .70)).roundToInt().coerceIn(2,6)
                (0 until count).map { part ->
                    (left+part*span/count) to (left+(part+1)*span/count-1)
                }
            } else listOf(left to right)
        }
        return groups.mapNotNull { (cx0, cx1) ->
            var ry0 = Int.MAX_VALUE
            var ry1 = Int.MIN_VALUE
            for (y in 0 until h) for (x in cx0..cx1) if (ink[y * w + x]) {
                if (y < ry0) ry0 = y
                if (y > ry1) ry1 = y
            }
            if (ry1 - ry0 + 1 < MIN_HEIGHT) return@mapNotNull null
            Glyph(
                normalize(ink, w, cx0, cx1, ry0, ry1),
                HudBox(x0 + cx0, y0 + ry0, x0 + cx1 + 1, y0 + ry1 + 1),
            )
        }
    }

    private fun columnGroups(ink: BooleanArray, w: Int, h: Int): List<Pair<Int, Int>> {
        val filled = BooleanArray(w)
        for (x in 0 until w) for (y in 0 until h) if (ink[y * w + x]) { filled[x] = true; break }
        val groups = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var gap = 0
        for (x in 0 until w) {
            if (filled[x]) {
                if (start < 0) start = x
                gap = 0
            } else if (start >= 0) {
                gap++
                if (gap > COLUMN_GAP) { groups += start to x - gap; start = -1; gap = 0 }
            }
        }
        if (start >= 0) groups += start to w - 1 - gap
        return groups
    }

    /** Rastert die Kontur der Glyphe auf [ROWS]x[COLS], unabhaengig von ihrer Pixelgroesse. */
    private fun normalize(ink: BooleanArray, w: Int, cx0: Int, cx1: Int, ry0: Int, ry1: Int): String {
        val sw = cx1 - cx0 + 1
        val sh = ry1 - ry0 + 1
        val bits = StringBuilder(ROWS * COLS)
        for (gr in 0 until ROWS) {
            val ya = gr * sh / ROWS
            val yb = maxOf(ya + 1, (gr + 1) * sh / ROWS)
            for (gc in 0 until COLS) {
                val xa = gc * sw / COLS
                val xb = maxOf(xa + 1, (gc + 1) * sw / COLS)
                var n = 0
                var on = 0
                for (y in ya until yb) for (x in xa until xb) {
                    n++
                    if (ink[(ry0 + y) * w + (cx0 + x)]) on++
                }
                bits.append(if (n > 0 && on * 100 / n >= CELL_ON) '1' else '0')
            }
        }
        return bits.toString()
    }

    private fun classify(bits: String): Int? {
        val ranked=TEMPLATES.groupBy { it.first }.map { (value, variants) ->
            value to variants.minOf { (_, template) -> bits.indices.count { bits[it] != template[it] } }
        }.sortedBy { it.second }
        val (digit,best)=ranked.firstOrNull() ?: return null
        val second=ranked.getOrNull(1)?.second ?: Int.MAX_VALUE
        if (best > MAX_DISTANCE) return null
        if (ranked.size > 1 && second - best < MIN_MARGIN) return null
        return digit
    }

    internal const val CLAWS_BAND = CLAWS_CENTER
    internal const val DASH_BAND = DASH_CENTER
    internal const val PAWS_BAND = .487

    private const val ONE =
        "000001111000" +   // .....####...
        "000011001100" +   // ....##..##..
        "001110001100" +   // ..###...##..
        "011000001100" +   // .##.....##..
        "110000001100" +   // ##......##..
        "010000001100" +   // .#......##..
        "011010001100" +   // .##.#...##..
        "001110001100" +   // ..###...##..
        "000110001100" +   // ...##...##..
        "000110001100" +   // ...##...##..
        "000110001100" +   // ...##...##..
        "001110001110" +   // ..###...###.
        "010000000001" +   // .#.........#
        "010000000001" +   // .#.........#
        "010000000001" +   // .#.........#
        "001111111110"     // ..#########.

    // Recorded from the verified green stock 271, never from the broom resource.
    private const val ONE_UPDATED = "000000110000000011001000000110001000111000001000100000001000100000001000010000001000001100001000000000001000000000001000000000001000000000001000011100001111010000000001010000000001011111111111"
    private const val SEVEN_UPDATED = "111111111111100000000001100000000001100000000001111111100001000001000001000001000010000010000110000100001000000100001000000100000000000100010000001000010000001000010000001000010000001111100000"
    private const val TWO_GREEN_UPDATED = "000011111000001100001110010000000011100000000001100000000001100011100001011110100001000001000001000001000001000110000010001000000100010000011111100000000001000000000001000000000001111111111111"
    private const val TWO_SHORT_UPDATED = "000011111000001100000110010000000011100000000001100000000001100011100001010110100001001100100001000001000001000001000001000110000110001000001100010000011111100000000001000000000001000000000001"
    private const val SEVEN_SHORT_UPDATED = "100000000001100000000001100000000001100000000001100000000001111111100001000001000001000011000111000010000101000100001001000100001001000100000001000100010000000100010000001000010000001000010000"
    private const val ZERO_UPDATED = "000111110000001100001100011000000110110000000010100000000001100001100001000010010001000010010001000010010001000010010001000010010001000010010001100001100000100000000001100000000001011000000110"

    private const val TWO =
        "000001110000" +   // .....###....
        "001111111100" +   // ..########..
        "011000000010" +   // .##.......#.
        "110000000011" +   // ##........##
        "110001100001" +   // ##...##....#
        "010011110001" +   // .#..####...#
        "001100100001" +   // ..##..#....#
        "000001100011" +   // .....##...##
        "000011000010" +   // ....##....#.
        "000110000110" +   // ...##....##.
        "001100001100" +   // ..##....##..
        "010000011111" +   // .#.....#####
        "110000000001" +   // ##.........#
        "110000000001" +   // ##.........#
        "110000000001" +   // ##.........#
        "011111111110"     // .##########.

    /** Updated game's outline glyph variant, recorded from the separate broom counter. */
    private const val TWO_UPDATED =
        "000011111000" +
        "001100000110" +
        "010000000001" +
        "100000000001" +
        "100001000001" +
        "110010100001" +
        "011100100001" +
        "000001000001" +
        "000001000001" +
        "000110000110" +
        "001000001100" +
        "010000011111" +
        "100000000001" +
        "000000000001" +
        "000000000001" +
        "111111111111"

    private const val THREE =
        "000111110000" +   // ...####.....
        "001111111100" +   // ..########..
        "010000000010" +   // .#........#.
        "110000000001" +   // ##.........#
        "010011100001" +   // .#..###....#
        "011111100001" +   // .#####.....#
        "001111000011" +   // ..####....##
        "001000000010" +   // ..#.......#.
        "001000000011" +   // ..#........#
        "001111000001" +   // ..####.....#
        "011111110001" +   // .#######...#
        "110111100001" +   // ##.####....#
        "110000000001" +   // ##.........#
        "110000000011" +   // ##........##
        "111000001110" +   // ###.....###.
        "001111111000"     // ..#######...
}

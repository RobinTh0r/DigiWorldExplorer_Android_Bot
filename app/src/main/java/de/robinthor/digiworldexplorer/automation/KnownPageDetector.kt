package de.robinthor.digiworldexplorer.automation

import de.robinthor.digiworldexplorer.vision.ColorComponent
import de.robinthor.digiworldexplorer.vision.ColorComponents
import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.PixelFrame

/** Geometry recognition for cyan-framed pages which share the game's standard chrome. */
object KnownPageDetector {
    private const val W = 180
    private const val H = 320

    fun detect(frame: PixelFrame, viewport: GameViewport = GameViewport.fit(frame.width, frame.height)): ObservedScreen {
        val list = de.robinthor.digiworldexplorer.dungeon.DungeonListDetector.detect(frame, viewport)
        if (list.position == de.robinthor.digiworldexplorer.dungeon.DungeonListPosition.TOP ||
            list.position == de.robinthor.digiworldexplorer.dungeon.DungeonListPosition.BOTTOM)
            return ObservedScreen.DUNGEON_LIST
        val cyan = BooleanArray(W * H)
        for (y in 0 until H) for (x in 0 until W) {
            val hsv = frame.rgbAt(viewport.left + x * viewport.width / W, viewport.top + y * viewport.height / H).hsv()
            cyan[y * W + x] = hsv.hue in 90..115 && hsv.saturation >= 90 && hsv.value >= 120
        }
        val components = ColorComponents.find(cyan, W, H).filter { it.pixels >= 90 }
        val largeHero = components.any {
            it.width.toDouble() / W in .68.. .82 && it.height.toDouble() / H >= .24 && centerY(it) in .20.. .36
        }
        val lowerRows = components.filter {
            it.width.toDouble() / W in .68.. .84 && it.height.toDouble() / H in .075.. .17 && centerY(it) in .40.. .86
        }
        if (largeHero && lowerRows.size >= 2) return ObservedScreen.PARTNER_PAGE
        if (lowerRows.size >= 3 && lowerRows.minOf(::centerY) < .48) {
            val centers = lowerRows.map(::centerY).sorted()
            if (centers.zipWithNext().count { (a, b) -> b - a in .09.. .22 } >= 2)
                return ObservedScreen.DUNGEON_LIST
        }
        return ObservedScreen.UNKNOWN
    }

    private fun centerY(component: ColorComponent) = (component.top + component.height / 2.0) / H
}

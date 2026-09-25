package de.robinthor.digiworldexplorer.automation

import android.media.Image
import de.robinthor.digiworldexplorer.dungeon.DungeonScreen
import de.robinthor.digiworldexplorer.dungeon.DungeonScreenDetector
import de.robinthor.digiworldexplorer.farm.ExploreMenuDetector
import de.robinthor.digiworldexplorer.farm.FarmDialogDetector
import de.robinthor.digiworldexplorer.farm.FarmHarvestDetector
import de.robinthor.digiworldexplorer.farm.FarmView
import de.robinthor.digiworldexplorer.purchase.RewardPurchaseDetector
import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.PixelFrame

/** Read-only fallback classifier. It never owns a frame and therefore can never authorize a tap. */
object PassiveScreenClassifier {
    fun detect(image: Image, width: Int, height: Int): ObservedScreen {
        val plane = image.planes.firstOrNull() ?: return ObservedScreen.UNKNOWN
        if (plane.pixelStride < 3) return ObservedScreen.UNKNOWN
        val buffer = plane.buffer
        val w = minOf(width, image.width)
        val h = minOf(height, image.height)
        if (w <= 0 || h <= 0 || (h - 1L) * plane.rowStride + (w - 1L) * plane.pixelStride + 2 >= buffer.limit())
            return ObservedScreen.UNKNOWN
        val frame = PixelFrame(w, h) { x, y ->
            val offset = y * plane.rowStride + x * plane.pixelStride
            (255 shl 24) or ((buffer.get(offset).toInt() and 255) shl 16) or
                ((buffer.get(offset + 1).toInt() and 255) shl 8) or (buffer.get(offset + 2).toInt() and 255)
        }
        val viewport = GameViewport.fit(w, h)
        val at: (Int, Int) -> Int = frame::argbAt
        val knownPage = KnownPageDetector.detect(frame, viewport)
        val explore = ExploreMenuDetector.detect(frame, viewport)
        val farm = if (knownPage == ObservedScreen.UNKNOWN && !explore.menu)
            FarmHarvestDetector.detect(frame, viewport) else de.robinthor.digiworldexplorer.farm.FarmHarvestDetection(false, emptyList())
        val farmDialog = if (!farm.field) FarmDialogDetector.detect(frame, farm.visiblePlots, viewport) else null
        return when {
            HomeScreenDetector.detect(w, h, at) -> ObservedScreen.HOME
            DungeonScreenDetector.detect(w, h, at).screen != DungeonScreen.NONE -> ObservedScreen.DUNGEON
            RewardPurchaseDetector.detect(w, h, at).recognized -> ObservedScreen.SUMMON
            farm.field -> ObservedScreen.MEAT_FIELD
            farmDialog?.view == FarmView.SEEDS || farmDialog?.view == FarmView.WATER -> ObservedScreen.MEAT_FIELD_DIALOG
            knownPage != ObservedScreen.UNKNOWN -> knownPage
            explore.menu -> ObservedScreen.EXPLORE_MENU
            else -> ObservedScreen.UNKNOWN
        }
    }
}

package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.detection.GridBounds
import org.junit.Assert.*
import org.junit.Test

class GridBoundsStabilityTest {
    @Test fun nativeLiveEdgeVariationDoesNotRevokeTheSameBoardAtDifferentScales() {
        for(scale in listOf(.5,1.0,1.5,2.0)) {
            fun b(l:Int,t:Int,r:Int,z:Int)=GridBounds((l*scale).toInt(),(t*scale).toInt(),(r*scale).toInt(),(z*scale).toInt())
            assertTrue(compatibleGridBounds(b(29,747,1154,1677),b(29,749,1154,1690)))
        }
    }
    @Test fun actualBoardTranslationOrResizeStillRevokesOldCoordinates() {
        val old=GridBounds(29,747,1154,1677)
        assertFalse(compatibleGridBounds(old,GridBounds(29,787,1154,1717)))
        assertFalse(compatibleGridBounds(old,GridBounds(29,747,1054,1677)))
    }
}

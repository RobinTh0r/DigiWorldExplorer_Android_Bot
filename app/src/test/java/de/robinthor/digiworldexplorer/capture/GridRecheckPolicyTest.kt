package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.detection.GridBounds
import org.junit.Assert.*
import org.junit.Test

class GridRecheckPolicyTest {
    private val original=GridBounds(69,926,1331,1963)
    private val other=GridBounds(69,719,1331,1756)
    @Test fun singleWrongRowPhaseBlocksInputWithoutErasingStableGrid() {
        val p=GridRecheckPolicy()
        assertEquals(GridRecheck.BLOCK,p.observe(original,other))
        assertEquals(GridRecheck.KEEP,p.observe(original,original))
    }
    @Test fun threeContinuouslyChangedBoundsRevokeCalibration() {
        val p=GridRecheckPolicy()
        assertEquals(GridRecheck.BLOCK,p.observe(original,other))
        assertEquals(GridRecheck.BLOCK,p.observe(original,other))
        assertEquals(GridRecheck.REVOKE,p.observe(original,other))
    }
    @Test fun alternatingWrongPhasesNeverAuthorizeInput() {
        val p=GridRecheckPolicy()
        for(i in 0..20)assertEquals(GridRecheck.BLOCK,p.observe(original,if(i%2==0)other else other.copy(top=800,bottom=1837)))
    }
    @Test fun missingBoardIsAlsoBoundedAndResetStartsFresh() {
        val p=GridRecheckPolicy()
        assertEquals(GridRecheck.BLOCK,p.observe(original,null))
        assertEquals(GridRecheck.BLOCK,p.observe(original,null))
        assertEquals(GridRecheck.REVOKE,p.observe(original,null))
        p.reset();assertEquals(GridRecheck.BLOCK,p.observe(original,null))
    }
}

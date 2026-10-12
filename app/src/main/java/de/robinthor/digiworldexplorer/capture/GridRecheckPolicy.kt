package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.detection.GridBounds

internal enum class GridRecheck { KEEP, BLOCK, REVOKE }
/** A single animated row-phase mismatch must block input, not erase all navigation state.
 * A genuinely changed/missing board still revokes calibration after continuous proof. */
internal class GridRecheckPolicy(private val confirmations:Int=3) {
    private var candidate:GridBounds?=null
    private var count=0
    fun reset(){candidate=null;count=0}
    fun observe(previous:GridBounds,fresh:GridBounds?):GridRecheck {
        if(fresh!=null && compatibleGridBounds(previous,fresh)){reset();return GridRecheck.KEEP}
        val same=if(fresh==null)candidate==null else candidate?.let {compatibleGridBounds(it,fresh)}==true
        count=if(same)count+1 else 1
        candidate=fresh
        return if(count>=confirmations)GridRecheck.REVOKE else GridRecheck.BLOCK
    }
}

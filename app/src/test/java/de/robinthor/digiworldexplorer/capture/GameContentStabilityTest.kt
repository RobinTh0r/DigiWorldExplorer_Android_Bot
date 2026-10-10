package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.vision.*
import org.junit.Assert.*
import org.junit.Test

class GameContentStabilityTest {
    private fun geometry(top:Int=0,dpi:Int=420,h:Int=2340)=FrameGeometry(PixelSize(1080,h),
        DisplayGeometry(PixelRect(0,0,1080,h),PixelRect(0,0,1080,h),dpi,0),PixelRect(0,top,1080,h))
    @Test fun flickeringBlackTopSeamsDoNotResetEveryQueuedGesture() {
        val stability=GameContentStability();val calibration=GeometryCalibration()
        var generation:Long?=null
        for(i in 0..120) {
            val frame=stability.observe(geometry(listOf(0,1,27,2)[i%4]),i*50L)
            val snapshot=calibration.observe(frame,i*50L)
            if(i>=2) {
                assertNotNull(snapshot)
                if(generation==null)generation=snapshot!!.generation
                assertEquals(generation,snapshot!!.generation)
                assertEquals(geometry(),snapshot.geometry)
            }
        }
    }
    @Test fun realStableLetterboxIsAdoptedAndVisibleGrowthIsImmediate() {
        val stability=GameContentStability(shrinkProofMs=750)
        stability.observe(geometry(),0)
        assertEquals(geometry(),stability.observe(geometry(100),1))
        assertEquals(geometry(),stability.observe(geometry(100),750))
        assertEquals(geometry(100),stability.observe(geometry(100),751))
        assertEquals(geometry(),stability.observe(geometry(),752))
    }
    @Test fun persistentSinglePixelSeamDoesNotInvalidateSubpixelAccurateInput() {
        val stability=GameContentStability();stability.observe(geometry(),0)
        for(i in 1..100) assertEquals(geometry(),stability.observe(geometry(i%2+1),i*100L))
    }
    @Test fun networkMenuTransitionWithPersistentBlackEdgeDoesNotRevokeActiveInput() {
        val stability=GameContentStability();val calibration=GeometryCalibration()
        var initial:GeometrySnapshot?=null
        for(i in 0..10) initial=calibration.observe(stability.observe(geometry(h=2424),i*50L),i*50L)
        val generation=initial!!.generation
        for(i in 11..45) {
            val observed=if(i<40)geometry(top=21,h=2424) else geometry(h=2424)
            val snapshot=calibration.observe(stability.observe(observed,i*50L),i*50L)
            assertNotNull(snapshot)
            assertEquals(generation,snapshot!!.generation)
            assertEquals(geometry(h=2424),snapshot.geometry)
        }
    }
    @Test fun densitySurfaceAndSessionChangesNeverKeepOldConfiguration() {
        val stability=GameContentStability();stability.observe(geometry(100),0)
        assertEquals(geometry(dpi=560),stability.observe(geometry(dpi=560),1))
        assertEquals(geometry(h=2400),stability.observe(geometry(h=2400),2))
        assertNull(stability.observe(null,3))
        assertEquals(geometry(27),stability.observe(geometry(27),4))
        stability.reset();assertEquals(geometry(),stability.observe(geometry(),5))
    }
    @Test fun runtimeRecognitionCannotRecropTheAlreadyCanonicalImage() {
        val snapshot=GeometrySnapshot(1,geometry(),0)
        val pixels=PixelFrame(1080,2340){_,y->if(y<27)0xff000000.toInt() else 0xff224466.toInt()}
        FrameGeometryRegistry.withSnapshot(snapshot) {
            assertEquals(GameViewport.fit(1080,2340),pixels.observedViewport)
        }
        val independent=PixelFrame(1080,2340){_,y->if(y<27)0xff000000.toInt() else 0xff224466.toInt()}
        assertEquals(GameViewport(0,27,1080,2313),independent.observedViewport)
    }
}

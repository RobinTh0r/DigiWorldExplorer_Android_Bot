package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class GeometryLifecycleTest {
    private fun geometry(w:Int=1080,h:Int=2340,dpi:Int=420,top:Int=0,rotation:Int=0)=FrameGeometry(PixelSize(w,h),DisplayGeometry(PixelRect(0,0,w,h),PixelRect(0,top,w,h-24),dpi,rotation),PixelRect(0,top,w,h-24))
    private fun confirm(c:GeometryCalibration,g:FrameGeometry,t:Long=10):GeometrySnapshot {c.observe(g,t);c.observe(g,t+1);return requireNotNull(c.observe(g,t+2))}
    @Test fun changesRevokeMappingImmediatelyAndNeedFreshStableObservations() {
        for(change in listOf(geometry(h=2400),geometry(dpi=640),geometry(top=72),geometry(w=2340,h=1080,rotation=1))) {
            val c=GeometryCalibration();val old=confirm(c,geometry())
            assertNull(c.observe(change,20));assertFalse(c.valid(old,20));assertNull(c.current(20))
            assertNull(c.observe(change,21));assertNotNull(c.observe(change,22))
        }
    }
    @Test fun startupResetNeverReusesPersistedOrPreviousSessionGeometry() {
        val c=GeometryCalibration();val old=confirm(c,geometry())
        c.reset();assertNull(c.current(13));assertFalse(c.valid(old,13));assertNull(c.observe(geometry(),14))
        assertNotEquals(old.generation,confirm(c,geometry(),15).generation)
    }
    @Test fun oldGenerationNeverBecomesValidAgainAfterAFrameGapEvenWithIdenticalGeometry() {
        val c=GeometryCalibration();val old=confirm(c,geometry())
        assertNull(c.observe(geometry(),3000));assertFalse(c.valid(old,3000))
        val fresh=confirm(c,geometry(),3001)
        assertNotEquals(old.generation,fresh.generation);assertFalse(c.valid(old,3003))
    }
    @Test fun thinOverlayIntersectionCannotBeMissedBetweenSwipeEndpoints() {
        val overlay=PixelRect(499,100,501,150)
        assertTrue(overlay.intersectsSegment(PixelPoint(0.0,125.0),PixelPoint(1000.0,125.0)))
        assertFalse(overlay.intersectsSegment(PixelPoint(0.0,125.0),PixelPoint(1000.0,900.0)))
        assertFalse(overlay.intersectsSegment(PixelPoint(500.0,0.0),PixelPoint(500.0,99.0)))
    }
    @Test fun noFramesClockRollbackOrBlackLoadingRevokesInput() {
        val c=GeometryCalibration();val old=confirm(c,geometry())
        assertNull(c.current(11));assertNull(c.current(2013))
        c.observe(null,14);assertFalse(c.valid(old,14))
        assertNull(VisibleGameArea.detect(PixelFrame(400,800){_,_->0xff000000.toInt()}))
    }
    @Test fun pendingBurstsCarryOriginalGenerationAndAreNotReinterpretedAfterResize() {
        val c=FrameGeometryRegistry.calibration;c.reset()
        try {
            var now=20L;var taps=0
            val old=confirm(c,geometry())
            val pending=FrameGeometryRegistry.withSnapshot(old){FrameGeometryRegistry.bind(now,{now}){taps++}}
            pending();assertEquals(1,taps)
            c.observe(geometry(h=2400),21);pending();assertEquals(1,taps)
            confirm(c,geometry(h=2400),22);now=25;pending();assertEquals(1,taps)
        } finally {c.reset()}
    }
    @Test fun cropHonoursRgbaRowAndPixelStrideAndKeepsReaderCursorsIndependent() {
        for(stride in listOf(4,8)) {
            val w=9;val h=8;val row=w*stride+16;val bytes=ByteBuffer.allocate(row*(h-1)+w*stride)
            for(y in 0 until h)for(x in 0 until w)for(channel in 0..3)bytes.put(y*row+x*stride+channel,(x+y*10+channel).toByte())
            val (image,_)=AnalysisImage.fromRgba(PixelSize(w,h),bytes,row,stride,PixelRect(2,3,8,7))
            assertEquals(6,image.width);assertEquals(4,image.height);assertEquals(24,image.planes[0].rowStride)
            val result=image.planes[0].buffer
            for(y in 0 until 4)for(x in 0 until 6)for(channel in 0..3)assertEquals((x+2+(y+3)*10+channel).toByte(),result.get(y*24+x*4+channel))
            result.position(20);assertEquals(0,image.planes[0].buffer.position())
            // Original last-row padding is absent: compaction still supplies a complete image.
            val (full,_)=AnalysisImage.fromRgba(PixelSize(w,h),bytes,row,stride,PixelRect(0,0,w,h))
            assertEquals(w*h*4,full.planes[0].buffer.remaining())
        }
    }
    @Test fun blackOuterBandsAreRemovedButInternalBlackGameplayIsNotCropped() {
        val frame=PixelFrame(600,900){x,y->if(x in 30 until 570 && y in 50 until 850 && !(x in 250..350 && y in 350..450))0xff335577.toInt() else 0xff000000.toInt()}
        assertEquals(PixelRect(30,50,570,850),VisibleGameArea.detect(frame))
        assertEquals(PixelRect(0,0,600,900),VisibleGameArea.detect(PixelFrame(600,900){x,y->if(x in 100..500 && y in 100..800)0xff000000.toInt() else 0xff111111.toInt()}))
    }
    @Test fun ownSmallOverlayIsNotMisidentifiedAsGameContentInBlackMargins() {
        val overlay=PixelRect(0,0,70,70)
        val frame=PixelFrame(600,1000){x,y->when {
            overlay.contains(x.toDouble(),y.toDouble())->0xff55aaaa.toInt()
            x in 90 until 510 && y in 110 until 890->0xff334477.toInt()
            else->0xff000000.toInt()
        }}
        assertEquals(PixelRect(90,110,510,890),VisibleGameArea.detect(frame,occlusions=listOf(overlay)))
    }
    @Test fun portraitGameOnLandscapeDisplayIsNotRejectedForOccupyingLessThan35Percent() {
        val frame=PixelFrame(2560,1600){x,y->if(x in 840 until 1720 && y in 0 until 1600)0xff334477.toInt() else 0xff000000.toInt()}
        assertEquals(PixelRect(840,0,1720,1600),VisibleGameArea.detect(frame))
    }
}

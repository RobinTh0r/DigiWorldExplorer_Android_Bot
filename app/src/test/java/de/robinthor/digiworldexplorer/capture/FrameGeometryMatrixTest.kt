package de.robinthor.digiworldexplorer.capture

import de.robinthor.digiworldexplorer.vision.PixelFrame
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs

/** Pure software display/projection simulation, not an Android device certification. */
@RunWith(Parameterized::class)
class FrameGeometryMatrixTest(private val width:Int,private val height:Int,private val navigation:Int,private val scale:Int) {
    private val display=PixelRect(0,0,width,height)
    private val window=PixelRect(0,if(navigation==0)0 else 27,width,height-navigation)
    private val capture=when(scale) {0->PixelSize(width,height);1->PixelSize(width/2,height/2);else->PixelSize(720,1280)}
    private val projection=ProjectionTransform(capture,display)
    private val game=PixelRect(width/12,window.top+window.height/10,width-width/17,window.bottom-window.height/14)
    private fun simulatedFrame()=PixelFrame(capture.width,capture.height) { x,y ->
        val p=projection.toDisplay(x.toDouble(),y.toDouble())
        when { game.contains(p.x,p.y)->0xff336699.toInt();window.contains(p.x,p.y)->0xff000000.toInt();else->0xff777777.toInt() }
    }
    private fun geometry():FrameGeometry {
        val content=requireNotNull(VisibleGameArea.detect(simulatedFrame(),requireNotNull(projection.captureRect(window))))
        return FrameGeometry(capture,DisplayGeometry(display,window,120+navigation*3,0),content)
    }
    @Test fun contentIsObservedRatherThanInferredFromFullDisplayAspect() {
        val actual=geometry().gameInCapture
        val expected=requireNotNull(projection.captureRect(game))
        assertTrue("actual=$actual expected=$expected",abs(actual.left-expected.left)<=1 && abs(actual.top-expected.top)<=1 && abs(actual.right-expected.right)<=1 && abs(actual.bottom-expected.bottom)<=1)
    }
    @Test fun detectedTargetsMapToPhysicalPixelsAndBackIncludingOffsets() {
        val geometry=geometry()
        for((nx,ny) in listOf(.03 to .02,.50 to .50,.97 to .98,.823 to .818)) {
            val x=nx*(geometry.analysisSize.width-1);val y=ny*(geometry.analysisSize.height-1)
            val point=requireNotNull(geometry.map(x,y))
            assertTrue(game.contains(point.x,point.y))
            val back=projection.toCapture(point.x,point.y)
            assertEquals(geometry.gameInCapture.left+x,back.x,1e-8)
            assertEquals(geometry.gameInCapture.top+y,back.y,1e-8)
        }
    }
    @Test fun clicksOutsideGameNeverGetClampedOntoNavigationOrAnotherButton() {
        val geometry=geometry()
        for((x,y) in listOf(-1.0 to 1.0,1.0 to -1.0,Double.NaN to 1.0,1.0 to Double.POSITIVE_INFINITY,geometry.analysisSize.width.toDouble() to 1.0,1.0 to geometry.analysisSize.height.toDouble()))assertNull(geometry.map(x,y))
    }
    @Test fun syntheticButtonChangesOnlyWhenMappedClickActuallyHitsIt() {
        val geometry=geometry();val cx=geometry.analysisSize.width*.72;val cy=geometry.analysisSize.height*.68
        val target=requireNotNull(geometry.map(cx,cy))
        // Physical hit-testing is independent of analysis coordinates; a raw screenshot tap
        // fails with crop/projection offsets, whereas the transformed point changes the state.
        val button=PixelRect((target.x-4).toInt(),(target.y-4).toInt(),(target.x+5).toInt(),(target.y+5).toInt())
        var screen="BEFORE"
        fun deliver(point:PixelPoint){if(button.contains(point.x,point.y))screen="CONFIRMED"}
        deliver(target);assertEquals("CONFIRMED",screen)
        if(abs(target.x-cx)>10 || abs(target.y-cy)>10){screen="BEFORE";deliver(PixelPoint(cx,cy));assertEquals("BEFORE",screen)}
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}x{1}, nav={2}, capture={3}")
        fun cases():List<Array<Int>> = listOf(1080 to 2340,1080 to 2400,720 to 1612,1440 to 3200,1080 to 1920,1536 to 2048,2560 to 1600,480 to 800).flatMap { (w,h)->listOf(0,24,144).flatMap { nav->(0..2).map { scale->arrayOf(w,h,nav,scale) } } }
    }
}

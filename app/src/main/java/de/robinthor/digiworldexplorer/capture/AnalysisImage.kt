package de.robinthor.digiworldexplorer.capture

import android.media.Image
import java.nio.ByteBuffer

/** Existing analyzers keep their Image-like interface, but every pixel is now in a
 * canonical game-local space. The Android frame remains alive until analysis ends. */
class AnalysisImage(val width:Int,val height:Int,val planes:Array<Plane>) {
    class Plane(private val data:ByteBuffer,val rowStride:Int,val pixelStride:Int) {
        val buffer:ByteBuffer get()=data.duplicate()
    }
    companion object {
        fun from(source:Image,crop:PixelRect,scratch:ByteBuffer?):Pair<AnalysisImage,ByteBuffer?> {
            val p=source.planes.first()
            return fromRgba(PixelSize(source.width,source.height),p.buffer,p.rowStride,p.pixelStride,crop,scratch)
        }
        fun fromRgba(size:PixelSize,data:ByteBuffer,rowStride:Int,pixelStride:Int,crop:PixelRect,scratch:ByteBuffer?=null):Pair<AnalysisImage,ByteBuffer?> {
            require(crop.left>=0 && crop.top>=0 && crop.right<=size.width && crop.bottom<=size.height)
            require(pixelStride>=4 && rowStride.toLong()>=size.width.toLong()*pixelStride)
            require((size.height-1L)*rowStride+(size.width-1L)*pixelStride+4<=data.limit())
            if(crop==PixelRect(0,0,size.width,size.height) && pixelStride==4 && rowStride==size.width*4)
                return AnalysisImage(size.width,size.height,arrayOf(Plane(data.duplicate(),rowStride,pixelStride))) to scratch
            val bytes=crop.width.toLong()*crop.height*4
            require(bytes<=Int.MAX_VALUE)
            val needed=bytes.toInt()
            val out=if(scratch!=null && scratch.capacity()>=needed)scratch else ByteBuffer.allocateDirect(needed)
            out.clear(); val input=data.duplicate()
            for(y in crop.top until crop.bottom) {
                val start=y*rowStride+crop.left*pixelStride
                if(pixelStride==4){input.limit(start+crop.width*4);input.position(start);out.put(input);input.clear()}
                else for(x in crop.left until crop.right)for(channel in 0..3)out.put(data.get(y*rowStride+x*pixelStride+channel))
            }
            out.flip()
            return AnalysisImage(crop.width,crop.height,arrayOf(Plane(out.asReadOnlyBuffer(),crop.width*4,4))) to out
        }
    }
}

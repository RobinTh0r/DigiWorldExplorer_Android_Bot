package de.robinthor.digiworldexplorer.capture

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/** Same Android display source for capture allocation and final input validation. */
object PhysicalDisplay {
    @Suppress("DEPRECATION")
    fun bounds(context:Context,legacy:Boolean=false):PixelRect {
        val manager=context.getSystemService(WindowManager::class.java)
        if(Build.VERSION.SDK_INT>=30 && !legacy) {
            val b=manager.maximumWindowMetrics.bounds
            return PixelRect(b.left,b.top,b.right,b.bottom)
        }
        val metrics=DisplayMetrics().also {manager.defaultDisplay.getRealMetrics(it)}
        return PixelRect(0,0,metrics.widthPixels,metrics.heightPixels)
    }
}

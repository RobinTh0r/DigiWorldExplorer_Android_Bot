package de.robinthor.digiworldexplorer.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class DigiWorldAccessibilityService : AccessibilityService() {
    override fun onServiceConnected(){ instance=this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy(){ if(instance===this) instance=null; super.onDestroy() }

    fun dispatchValidatedTap(x:Float,y:Float,onComplete:(Boolean)->Unit){
        if(x<0f||y<0f){onComplete(false);return}
        val path=Path().apply{moveTo(x,y)}
        val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,80)).build()
        val accepted=dispatchGesture(gesture,object:GestureResultCallback(){
            override fun onCompleted(g:GestureDescription?)=onComplete(true)
            override fun onCancelled(g:GestureDescription?)=onComplete(false)
        },null)
        if(!accepted)onComplete(false)
    }
    companion object { @Volatile var instance:DigiWorldAccessibilityService?=null; private set }
}
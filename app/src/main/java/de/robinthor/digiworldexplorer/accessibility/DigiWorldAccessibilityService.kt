package de.robinthor.digiworldexplorer.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class DigiWorldAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /**
     * Controlled development-only gesture primitive. It is never called by the
     * capture service or strategy automatically; a later explicit user action
     * must invoke it after coordinate validation.
     */
    fun dispatchValidatedTap(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
        if (x < 0f || y < 0f) {
            onComplete(false)
            return
        }
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = onComplete(true)
                override fun onCancelled(gestureDescription: GestureDescription?) = onComplete(false)
            },
            null,
        )
        if (!accepted) onComplete(false)
    }
}

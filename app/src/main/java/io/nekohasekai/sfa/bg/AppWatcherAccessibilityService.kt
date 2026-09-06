package io.nekohasekai.sfa.bg

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppWatcherAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppWatcherA11y"
        var isConnected: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        Log.i(TAG, "Accessibility service connected")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 50
        serviceInfo = info

        AppForegroundWatcher.onAccessibilityServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d(TAG, "onAccessibilityEvent: window changed to $packageName")
            AppForegroundWatcher.onForegroundPackageChanged(packageName)
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        isConnected = false
        AppForegroundWatcher.onAccessibilityServiceDisconnected()
        super.onDestroy()
    }
}

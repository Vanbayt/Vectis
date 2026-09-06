package io.nekohasekai.sfa.bg

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import io.nekohasekai.sfa.constant.ServiceMode
import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.CoroutineScope

object AppForegroundWatcher {
    private const val TAG = "AppForegroundWatcher"

    private var isCurrentlyPausedByApp = false
    private var lastExcludedPackage: String? = null
    private val appPackageName: String by lazy { io.nekohasekai.sfa.Application.application.packageName }

    // System packages that shouldn't cause an immediate exit/resume if temporarily displayed (e.g. keyboard, system UI, volume bar)
    private val TRANSIENT_SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.sohu.inputmethod.sogou",
        "com.touchtype.swiftkey",
        "com.coloros.smartsidebar",
        "com.coloros.assistantscreen",
        "com.oplus.smartsidebar",
        "com.oplus.nhs",
    )

    // Callbacks for BoxService / VPNService
    var onPauseListener: ((String) -> Unit)? = null
    var onResumeListener: (() -> Unit)? = null

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (AppWatcherAccessibilityService.isConnected) return true

        val expectedComponentName = "${context.packageName}/${AppWatcherAccessibilityService::class.java.name}"
        val expectedShortName = "${context.packageName}/.bg.AppWatcherAccessibilityService"

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am != null) {
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo?.serviceInfo ?: continue
                if (serviceInfo.packageName == context.packageName &&
                    (serviceInfo.name == AppWatcherAccessibilityService::class.java.name ||
                        serviceInfo.name.endsWith("AppWatcherAccessibilityService"))
                ) {
                    return true
                }
            }
        }

        val enabledServicesSetting = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true) ||
                componentName.equals(expectedShortName, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    fun isEnabled(): Boolean {
        // Auto-pause is strictly for standard VpnService mode.
        // In Root Mode (Native TUN), VPN is invisible to banking apps at the kernel level.
        return Settings.serviceMode != ServiceMode.ROOT_TUN &&
            Settings.perAppProxyEnabled &&
            Settings.autoPauseOnExcludedApps &&
            Settings.getEffectivePerAppProxyMode() == Settings.PER_APP_PROXY_EXCLUDE
    }

    fun start(context: Context, scope: CoroutineScope? = null) {
        isCurrentlyPausedByApp = false
        lastExcludedPackage = null
        Log.i(TAG, "Foreground watcher initialized (Event-Driven Accessibility mode)")
    }

    fun onAccessibilityServiceConnected() {
        Log.i(TAG, "Accessibility service attached to watcher")
    }

    fun onAccessibilityServiceDisconnected() {
        Log.i(TAG, "Accessibility service detached from watcher")
    }

    fun onForegroundPackageChanged(packageName: String) {
        // Strict requirement: If Root Mode is active or feature is disabled, completely ignore events!
        if (Settings.serviceMode == ServiceMode.ROOT_TUN) {
            return
        }
        val enabled = isEnabled()
        if (!enabled) {
            Log.d(TAG, "Event ignored for $packageName: auto-pause not enabled (autoPause=${Settings.autoPauseOnExcludedApps}, perAppProxy=${Settings.perAppProxyEnabled}, serviceMode=${Settings.serviceMode}, proxyMode=${Settings.getEffectivePerAppProxyMode()})")
            return
        }

        // Ignore transient overlays (keyboards, system UI, volume panels)
        if (TRANSIENT_SYSTEM_PACKAGES.contains(packageName)) {
            Log.v(TAG, "Event ignored for transient package: $packageName")
            return
        }

        val excludedPackages = Settings.getEffectivePerAppProxyList()
        val isExcluded = excludedPackages.contains(packageName)
        Log.i(TAG, "Foreground package changed: $packageName (isExcluded=$isExcluded, isPaused=$isCurrentlyPausedByApp)")

        if (isExcluded) {
            if (!isCurrentlyPausedByApp) {
                isCurrentlyPausedByApp = true
                lastExcludedPackage = packageName
                Log.i(TAG, "Excluded app window focused: $packageName -> Pausing VPN")
                onPauseListener?.invoke(packageName)
            }
        } else {
            // When user switches to any non-excluded app, launcher or Vectis
            if (isCurrentlyPausedByApp) {
                isCurrentlyPausedByApp = false
                lastExcludedPackage = null
                Log.i(TAG, "Left excluded app to $packageName -> Resuming VPN")
                onResumeListener?.invoke()
            }
        }
    }

    fun stop() {
        isCurrentlyPausedByApp = false
        lastExcludedPackage = null
        onPauseListener = null
        onResumeListener = null
        Log.i(TAG, "Stopped foreground watcher")
    }
}

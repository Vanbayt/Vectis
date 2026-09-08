package io.nekohasekai.sfa.bg

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.vendor.Vendor
import kotlinx.coroutines.runBlocking

class ProxyService :
    Service(),
    PlatformInterfaceWrapper {
    companion object {
        private const val TAG = "ProxyService"
        private const val ROOT_TUN_IF_NAME = "wlan2"
    }

    private val service = BoxService(this, this)
    private var nativeTunPfd: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = service.onStartCommand()

    override fun onBind(intent: Intent) = service.onBind()

    override fun onDestroy() {
        closeNativeTun()
        service.onDestroy()
    }

    override fun sendNotification(notification: Notification) = service.sendNotification(notification)

    override fun autoDetectInterfaceControl(fd: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val network = Application.connectivity.activeNetwork
                if (network != null) {
                    val fileDescriptor = java.io.FileDescriptor().apply {
                        val field = java.io.FileDescriptor::class.java.getDeclaredField("descriptor")
                        field.isAccessible = true
                        field.setInt(this, fd)
                    }
                    network.bindSocket(fileDescriptor)
                }
            }.onFailure {
                Log.w(TAG, "Failed to bind socket to active network", it)
            }
        }
    }

    override fun openTun(options: TunOptions): Int {
        Log.i(TAG, "Opening Native TUN via Root Server...")
        val includeUids = mutableListOf<Int>()
        val excludeUids = mutableListOf<Int>()

        if (Vendor.isPerAppProxyAvailable() && Settings.perAppProxyEnabled) {
            val appList = Settings.getEffectivePerAppProxyList()
            val mode = Settings.getEffectivePerAppProxyMode()
            for (pkg in appList) {
                if (pkg == packageName) continue
                try {
                    val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        packageManager.getPackageUid(pkg, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getApplicationInfo(pkg, 0).uid
                    }
                    if (uid > 0) {
                        if (mode == Settings.PER_APP_PROXY_INCLUDE) {
                            includeUids.add(uid)
                        } else {
                            excludeUids.add(uid)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resolve UID for package: $pkg", e)
                }
            }
        }

        Log.i(
            TAG,
            "Native TUN PerAppProxy: enabled=${Settings.perAppProxyEnabled}, include=${includeUids.size}, exclude=${excludeUids.size}",
        )

        val pfd = runBlocking {
            RootClient.openNativeTun(
                ROOT_TUN_IF_NAME,
                options.mtu,
                includeUids.toIntArray(),
                excludeUids.toIntArray(),
            )
        }
        nativeTunPfd = pfd
        service.fileDescriptor = pfd
        return pfd.fd
    }

    private fun closeNativeTun() {
        nativeTunPfd?.let {
            runCatching { it.close() }
            nativeTunPfd = null
        }
        runBlocking {
            runCatching { RootClient.closeNativeTun(ROOT_TUN_IF_NAME) }
        }
    }
}


package io.nekohasekai.sfa.bg

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.sfa.Application
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
        val pfd = runBlocking {
            RootClient.openNativeTun(ROOT_TUN_IF_NAME, options.mtu)
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


package io.nekohasekai.sfa.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

object UdpProber {
    private const val TAG = "UdpProber"
    private const val PACKET_TIMEOUT_MS = 600

    // DNS A query for example.com
    private val DNS_PROBE_QUERY = byteArrayOf(
        0xaa.toByte(), 0xbb.toByte(),
        0x01, 0x00,
        0x00, 0x01,
        0x00, 0x00,
        0x00, 0x00,
        0x00, 0x00,
        0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
        0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
        0x00,
        0x00, 0x01,
        0x00, 0x01
    )

    // STUN Binding Request (RFC 5389)
    private val STUN_PROBE_REQUEST = byteArrayOf(
        0x00, 0x01, 0x00, 0x00,
        0x21, 0x12, 0xa4.toByte(), 0x42,
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c
    )

    enum class UdpState {
        UNKNOWN,
        UDP_HEALTHY,
        UDP_SHAPED
    }

    private val _state = MutableStateFlow(UdpState.UNKNOWN)
    val state: StateFlow<UdpState> get() = _state

    val currentState: UdpState get() = _state.value

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastNetworkType: Int? = null

    /**
     * Initializes network monitoring callbacks to detect Wi-Fi / Cellular changes
     * and trigger UDP probing automatically.
     */
    fun startMonitoring(context: Context, probeTargetHost: String? = null, probeTargetPort: Int = 443) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        if (networkCallback != null) return // Already monitoring

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val currentType = when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                    else -> -1
                }

                if (lastNetworkType != null && lastNetworkType != currentType) {
                    Log.i(TAG, "Network interface changed ($lastNetworkType -> $currentType). Triggering UDP probe.")
                    probeNetwork(probeTargetHost ?: "8.8.8.8", probeTargetPort)
                }
                lastNetworkType = currentType
            }
        }

        networkCallback = callback
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }
    }

    fun stopMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback: ${e.message}", e)
            }
            networkCallback = null
        }
    }

    /**
     * Probes UDP connectivity using DNS and STUN query probes.
     */
    fun probeNetwork(host: String? = null, port: Int = 53, onComplete: ((UdpState) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = executeProbe()
            _state.value = result
            Log.i(TAG, "UDP Probe result -> $result")
            AppLogCollector.appendLog(TAG, "[UdpProber] Evaluated state -> $result")
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result)
            }
        }
    }

    private suspend fun executeProbe(): UdpState = withContext(Dispatchers.IO) {
        // Multi-target probe: 1.1.1.1:53 (Cloudflare DNS), 77.88.8.8:53 (Yandex DNS in RU), stun.cloudflare.com:3478 (STUN)
        val targets = listOf(
            Triple("1.1.1.1", 53, DNS_PROBE_QUERY),
            Triple("77.88.8.8", 53, DNS_PROBE_QUERY),
            Triple("stun.cloudflare.com", 3478, STUN_PROBE_REQUEST)
        )

        for ((targetHost, targetPort, payload) in targets) {
            try {
                val address = InetAddress.getByName(targetHost)
                val socket = DatagramSocket().apply {
                    soTimeout = PACKET_TIMEOUT_MS
                }
                socket.use { ds ->
                    val sendPacket = DatagramPacket(payload, payload.size, address, targetPort)
                    ds.send(sendPacket)

                    val receiveBuffer = ByteArray(512)
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    ds.receive(receivePacket)
                    if (receivePacket.length > 0) {
                        Log.d(TAG, "UDP probe target $targetHost:$targetPort answered (${receivePacket.length} bytes)")
                        return@withContext UdpState.UDP_HEALTHY
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "UDP probe target $targetHost:$targetPort failed: ${e.message}")
            }
        }

        Log.w(TAG, "All UDP probe targets failed. Network is UDP_SHAPED.")
        return@withContext UdpState.UDP_SHAPED
    }
}

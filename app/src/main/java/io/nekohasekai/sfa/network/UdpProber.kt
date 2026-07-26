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
    private const val PROBE_PACKETS_COUNT = 20
    private const val PACKET_TIMEOUT_MS = 500

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
     * Sends a batch of 20 UDP probe packets to test packet loss and latency.
     */
    fun probeNetwork(host: String, port: Int = 443, onComplete: ((UdpState) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = executeProbe(host, port)
            _state.value = result
            Log.i(TAG, "UDP Probe result for $host:$port -> $result")
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result)
            }
        }
    }

    private suspend fun executeProbe(host: String, port: Int): UdpState = withContext(Dispatchers.IO) {
        var receivedCount = 0
        var totalLatency = 0L

        try {
            val address = InetAddress.getByName(host)
            val socket = DatagramSocket().apply {
                soTimeout = PACKET_TIMEOUT_MS
            }

            socket.use { ds ->
                for (seq in 1..PROBE_PACKETS_COUNT) {
                    val sendData = "VectisProbe:$seq:${System.currentTimeMillis()}".toByteArray(Charsets.UTF_8)
                    val sendPacket = DatagramPacket(sendData, sendData.size, address, port)

                    val startTime = System.currentTimeMillis()
                    try {
                        ds.send(sendPacket)

                        val receiveData = ByteArray(512)
                        val receivePacket = DatagramPacket(receiveData, receiveData.size)
                        ds.receive(receivePacket)

                        val rtt = System.currentTimeMillis() - startTime
                        receivedCount++
                        totalLatency += rtt
                    } catch (e: SocketTimeoutException) {
                        // Packet dropped or timed out
                    } catch (e: Exception) {
                        Log.w(TAG, "Probe packet $seq error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "UDP Probe execution failed: ${e.message}", e)
            return@withContext UdpState.UDP_SHAPED
        }

        val lossRate = (PROBE_PACKETS_COUNT - receivedCount).toDouble() / PROBE_PACKETS_COUNT.toDouble()
        Log.d(TAG, "UDP Probe stats: Received $receivedCount/$PROBE_PACKETS_COUNT, Loss: ${lossRate * 100}%")

        if (receivedCount > 0 && lossRate <= 0.15) {
            UdpState.UDP_HEALTHY
        } else {
            UdpState.UDP_SHAPED
        }
    }
}

package io.nekohasekai.sfa.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * PreConnectPingManager performs lightweight direct TCP & UDP socket RTT measurements
 * to server endpoints OUTSIDE / BEFORE the VPN service is connected.
 * 
 * Fully dynamic: Accepts endpoints parsed directly from active profile JSON.
 */
object PreConnectPingManager {
    private const val TAG = "VectisHealth"
    private const val CONNECT_TIMEOUT_MS = 1500

    private val _pingResults = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pingResults: StateFlow<Map<String, Int>> = _pingResults.asStateFlow()

    private var isProbing = false

    data class ServerEndpoint(
        val tag: String,
        val host: String,
        val port: Int,
        val isUdp: Boolean = false
    )

    fun getPingForTag(tag: String): Int {
        return _pingResults.value[tag] ?: 0
    }

    fun probeAll(endpoints: List<ServerEndpoint>, onComplete: ((Map<String, Int>) -> Unit)? = null) {
        if (endpoints.isEmpty() || isProbing) return
        isProbing = true

        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "[PreConnectPing] Dynamic probe starting for ${endpoints.size} endpoints: ${endpoints.map { it.tag }}")
            val results = mutableMapOf<String, Int>()
            val jobs = endpoints.map { ep ->
                launch {
                    val rtt = if (ep.isUdp) {
                        measureUdpRtt(ep.host, ep.port)
                    } else {
                        measureTcpRtt(ep.host, ep.port)
                    }
                    synchronized(results) {
                        results[ep.tag] = rtt
                    }
                }
            }
            jobs.forEach { it.join() }

            val updatedMap = _pingResults.value.toMutableMap().apply { putAll(results) }
            _pingResults.value = updatedMap
            isProbing = false
            AppLogCollector.appendLog(TAG, "[PreConnectPing] Dynamic probe completed -> $updatedMap")

            withContext(Dispatchers.Main) {
                onComplete?.invoke(updatedMap)
            }
        }
    }

    private suspend fun measureTcpRtt(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            val rtt = (System.currentTimeMillis() - startTime).toInt()
            socket.close()
            Log.d(TAG, "[PreConnectPing] TCP RTT for $host:$port ($rtt ms)")
            return@withContext rtt.coerceAtLeast(1)
        } catch (e: Exception) {
            Log.w(TAG, "[PreConnectPing] TCP RTT failed for $host:$port -> ${e.message}")
            return@withContext 0
        }
    }

    private suspend fun measureUdpRtt(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val address = InetAddress.getByName(host)
            val socket = DatagramSocket()
            socket.use { ds ->
                val payload = byteArrayOf(0x00, 0x01, 0x02, 0x03)
                val packet = DatagramPacket(payload, payload.size, address, port)
                ds.send(packet)
            }
            val rtt = (System.currentTimeMillis() - startTime).toInt()
            Log.d(TAG, "[PreConnectPing] UDP RTT for $host:$port ($rtt ms)")
            return@withContext rtt.coerceAtLeast(1)
        } catch (e: Exception) {
            Log.w(TAG, "[PreConnectPing] UDP RTT failed for $host:$port -> ${e.message}")
            return@withContext 0
        }
    }
}

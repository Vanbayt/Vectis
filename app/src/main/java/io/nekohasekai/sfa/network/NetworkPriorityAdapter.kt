package io.nekohasekai.sfa.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * NetworkPriorityAdapter analyzes active network transport (Wi-Fi vs Cellular 4G vs 3G/2G)
 * and combines it with UDP health status from UdpProber to dynamically adapt outbound priority.
 */
object NetworkPriorityAdapter {
    private const val TAG = "VectisHealth"

    enum class NetworkCategory {
        WIFI,
        CELLULAR_4G_5G,
        CELLULAR_WEAK_3G_2G,
        UNKNOWN
    }

    fun getActiveNetworkCategory(context: Context): NetworkCategory {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkCategory.UNKNOWN

        val activeNetwork = cm.activeNetwork ?: return NetworkCategory.UNKNOWN
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkCategory.UNKNOWN

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCategory.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val downstreamKbps = caps.linkDownstreamBandwidthKbps
                if (downstreamKbps in 1..2500) {
                    NetworkCategory.CELLULAR_WEAK_3G_2G
                } else {
                    NetworkCategory.CELLULAR_4G_5G
                }
            }
            else -> NetworkCategory.UNKNOWN
        }
    }

    /**
     * Sorts a list of proxy tags based on active network category and UDP health state.
     */
    fun sortProxyTags(
        proxyTags: List<String>,
        context: Context,
        udpState: UdpProber.UdpState = UdpProber.currentState
    ): List<String> {
        val category = getActiveNetworkCategory(context)
        AppLogCollector.appendLog(TAG, "[NetworkAdapter] Active Network: $category | UDP State: $udpState | Raw Outbounds: $proxyTags")

        val sorted = proxyTags.sortedBy { tag ->
            val isHysteria = tag.contains("hysteria", ignoreCase = true) || tag.contains("hy2", ignoreCase = true) || tag.endsWith("-9") || tag.endsWith("-2") || tag.endsWith("-8")
            val isGrpc = tag.contains("grpc", ignoreCase = true) || tag.endsWith("-5") || tag.endsWith("-3")
            val isTcp = !isHysteria && !isGrpc

            if (udpState == UdpProber.UdpState.UDP_SHAPED && isHysteria) {
                // If UDP is blocked/shaped by TSPU, demote Hysteria 2 to bottom regardless of transport
                3
            } else {
                when (category) {
                    NetworkCategory.CELLULAR_4G_5G, NetworkCategory.CELLULAR_WEAK_3G_2G -> {
                        when {
                            isGrpc -> 0
                            isTcp -> 1
                            else -> 2
                        }
                    }
                    NetworkCategory.WIFI -> {
                        if (udpState == UdpProber.UdpState.UDP_HEALTHY) {
                            when {
                                isHysteria -> 0
                                isTcp -> 1
                                else -> 2
                            }
                        } else {
                            when {
                                isTcp -> 0
                                isGrpc -> 1
                                else -> 2
                            }
                        }
                    }
                    NetworkCategory.UNKNOWN -> {
                        if (udpState == UdpProber.UdpState.UDP_SHAPED) {
                            if (isHysteria) 2 else 0
                        } else {
                            0
                        }
                    }
                }
            }
        }

        AppLogCollector.appendLog(TAG, "[NetworkAdapter] Optimized Priority Order -> $sorted")
        return sorted
    }
}

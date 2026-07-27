package io.nekohasekai.sfa.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * NetworkPriorityAdapter analyzes active network transport (Wi-Fi vs Cellular)
 * and combines it with UDP health status from UdpProber and PreConnect Ping metrics
 * to dynamically score and prioritize proxy outbounds.
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
     * Sorts a list of proxy tags based on multi-metric composite scoring:
     * - Wi-Fi: Hysteria 2 (UDP/QUIC + BBR) is preferred #1 for packet-loss tolerance & max throughput.
     * - Cellular (UDP Shaped): VLESS gRPC / Reality TCP preferred #1 for DPI evasion, Hysteria 2 demoted.
     * - PreConnect Ping RTT incorporated with logarithmic scaling.
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

            val pingRtt = PreConnectPingManager.getPingForTag(tag)

            // Base transport rank (0 = highest priority, higher = lower priority)
            val baseRank = if (udpState == UdpProber.UdpState.UDP_SHAPED && isHysteria) {
                // TSPU blocking UDP on cellular -> demote Hysteria 2 to last place
                100
            } else {
                when (category) {
                    NetworkCategory.WIFI -> {
                        // Wi-Fi network: Hysteria 2 (UDP QUIC + BBR) #0 (highest speed & loss resilience), Reality TCP #1, gRPC #2
                        when {
                            isHysteria -> 0
                            isTcp -> 1
                            else -> 2
                        }
                    }
                    NetworkCategory.CELLULAR_4G_5G, NetworkCategory.CELLULAR_WEAK_3G_2G -> {
                        // Cellular network: VLESS gRPC #0 (DPI evasion), VLESS Reality TCP #1, Hysteria 2 #2
                        when {
                            isGrpc -> 0
                            isTcp -> 1
                            else -> 2
                        }
                    }
                    NetworkCategory.UNKNOWN -> {
                        when {
                            isHysteria -> 0
                            isTcp -> 1
                            else -> 2
                        }
                    }
                }
            }

            // Add scaled ping penalty (100ms ping adds +1 to rank score, preventing minor RTT differences from overriding protocol selection)
            val pingPenalty = if (pingRtt > 0) (pingRtt / 100.0) else 0.0
            val compositeScore = baseRank + pingPenalty

            compositeScore
        }

        AppLogCollector.appendLog(TAG, "[NetworkAdapter] Multi-Metric Optimized Priority Order -> $sorted")
        return sorted
    }
}

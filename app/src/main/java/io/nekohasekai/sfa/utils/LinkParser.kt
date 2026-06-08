package io.nekohasekai.sfa.utils

import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder

object LinkParser {

    fun parse(url: String): String? {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("vless://") -> parseVless(trimmed)
            trimmed.startsWith("hysteria2://") || trimmed.startsWith("hy2://") -> parseHysteria2(trimmed)
            trimmed.startsWith("trojan://") -> parseTrojan(trimmed)
            trimmed.startsWith("ss://") -> parseShadowsocks(trimmed)
            trimmed.startsWith("vmess://") -> parseVmess(trimmed)
            else -> null
        }
    }

    private fun generateSingBoxConfig(outboundJson: String, serverIP: String): String {
        return """
        {
          "log": { "level": "warn", "timestamp": true },
          "dns": {
            "servers": [
              { "tag": "google-dns", "type": "udp", "server": "8.8.8.8", "detour": "proxy" }
            ]
          },
          "inbounds": [
            {
              "type": "tun",
              "tag": "tun-in",
              "interface_name": "tun0",
              "address": ["172.19.0.1/30", "fdfe:dcba:9876::1/126"],
              "auto_route": true,
              "strict_route": true
            }
          ],
          "outbounds": [
            $outboundJson,
            { "type": "direct", "tag": "direct" },
            { "type": "block", "tag": "block" }
          ],
          "route": {
            "rules": [
              { "action": "sniff" },
              { "protocol": "dns", "action": "hijack-dns" },
              { "ip_is_private": true, "outbound": "direct" },
              { "ip_cidr": ["$serverIP/32"], "outbound": "direct" }
            ],
            "auto_detect_interface": true,
            "final": "proxy"
          }
        }
        """.trimIndent()
    }

    private fun parseVless(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val uuid = uri.userInfo ?: return null
            val server = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            
            val type = uri.getQueryParameter("type") ?: "tcp"
            val security = uri.getQueryParameter("security") ?: "none"
            val flow = uri.getQueryParameter("flow")
            val pbk = uri.getQueryParameter("pbk")
            val sni = uri.getQueryParameter("sni")
            val fp = uri.getQueryParameter("fp") ?: "chrome"
            val sid = uri.getQueryParameter("sid")

            val outbound = """
            {
              "type": "vless",
              "tag": "proxy",
              "server": "$server",
              "server_port": $port,
              "uuid": "$uuid",
              ${if (!flow.isNullOrEmpty()) "\"flow\": \"$flow\"," else ""}
              "packet_encoding": "xudp",
              "tls": {
                "enabled": ${if (security == "tls" || security == "reality") "true" else "false"},
                "server_name": "${sni ?: server}",
                "utls": { "enabled": true, "fingerprint": "$fp" }
                ${if (security == "reality" && !pbk.isNullOrEmpty()) """,
                "reality": { "enabled": true, "public_key": "$pbk", "short_id": "${sid ?: ""}" }""" else ""}
              }
            }
            """.trimIndent()

            return generateSingBoxConfig(outbound, server)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseHysteria2(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val password = uri.userInfo ?: return null
            val server = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            
            val sni = uri.getQueryParameter("sni")
            val insecure = uri.getQueryParameter("insecure") == "1"
            val obfs = uri.getQueryParameter("obfs")
            val obfsPassword = uri.getQueryParameter("obfs-password")

            val outbound = """
            {
              "type": "hysteria2",
              "tag": "proxy",
              "server": "$server",
              "server_port": $port,
              "password": "$password",
              "up_mbps": 100,
              "down_mbps": 100,
              "tls": {
                "enabled": true,
                "server_name": "${sni ?: server}",
                "insecure": $insecure
              }
              ${if (obfs == "salamander" && !obfsPassword.isNullOrEmpty()) """,
              "obfs": { "type": "salamander", "password": "$obfsPassword" }""" else ""}
            }
            """.trimIndent()

            return generateSingBoxConfig(outbound, server)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseTrojan(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val password = uri.userInfo ?: return null
            val server = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            
            val sni = uri.getQueryParameter("sni")
            val security = uri.getQueryParameter("security") ?: "tls"
            val type = uri.getQueryParameter("type") ?: "tcp"
            val fp = uri.getQueryParameter("fp") ?: "chrome"

            val outbound = """
            {
              "type": "trojan",
              "tag": "proxy",
              "server": "$server",
              "server_port": $port,
              "password": "$password",
              "tls": {
                "enabled": ${if (security == "tls") "true" else "false"},
                "server_name": "${sni ?: server}",
                "utls": { "enabled": true, "fingerprint": "$fp" }
              }
            }
            """.trimIndent()

            return generateSingBoxConfig(outbound, server)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseShadowsocks(url: String): String? {
        try {
            val uri = Uri.parse(url)
            var userInfo = uri.userInfo
            var server = uri.host
            var port = uri.port
            
            if (userInfo == null) {
                // Legacy base64 format without @ natively parsed by URI
                val hostPart = url.substringAfter("ss://").substringBefore("#")
                val decoded = String(Base64.decode(hostPart, Base64.DEFAULT))
                val methodPass = decoded.substringBefore("@")
                val hostPort = decoded.substringAfter("@")
                userInfo = methodPass
                server = hostPort.substringBefore(":")
                port = hostPort.substringAfter(":").toIntOrNull() ?: 8388
            } else {
                val decodedUserInfo = String(Base64.decode(userInfo, Base64.DEFAULT))
                if (decodedUserInfo.contains(":")) {
                    userInfo = decodedUserInfo
                }
            }

            val method = userInfo?.substringBefore(":") ?: return null
            val password = userInfo.substringAfter(":")
            if (server == null) return null
            if (port == -1) port = 8388

            val outbound = """
            {
              "type": "shadowsocks",
              "tag": "proxy",
              "server": "$server",
              "server_port": $port,
              "method": "$method",
              "password": "$password"
            }
            """.trimIndent()

            return generateSingBoxConfig(outbound, server)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseVmess(url: String): String? {
        try {
            val base64Data = url.substringAfter("vmess://")
            val jsonString = String(Base64.decode(base64Data, Base64.DEFAULT))
            val json = JSONObject(jsonString)

            val server = json.optString("add")
            val port = json.optInt("port", 443)
            val uuid = json.optString("id")
            val security = json.optString("scy", "auto")
            val tls = json.optString("tls")
            val sni = json.optString("sni", server)

            val outbound = """
            {
              "type": "vmess",
              "tag": "proxy",
              "server": "$server",
              "server_port": $port,
              "uuid": "$uuid",
              "security": "$security",
              "tls": {
                "enabled": ${if (tls == "tls") "true" else "false"},
                "server_name": "$sni",
                "utls": { "enabled": true, "fingerprint": "chrome" }
              }
            }
            """.trimIndent()

            return generateSingBoxConfig(outbound, server)
        } catch (e: Exception) {
            return null
        }
    }
}

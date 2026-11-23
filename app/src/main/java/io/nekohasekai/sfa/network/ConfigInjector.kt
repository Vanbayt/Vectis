package io.nekohasekai.sfa.network

import org.json.JSONArray
import org.json.JSONObject

object ConfigInjector {

    private val BLOCKED_DOMAINS = listOf(
        "instagram.com", "cdninstagram.com",
        "facebook.com", "fbcdn.net", "meta.com",
        "twitter.com", "x.com", "t.co", "twimg.com",
        "discord.com", "discordapp.com", "discord.gg", "discordapp.net",
        "openai.com", "chatgpt.com", "oaistatic.com", "oaiusercontent.com",
        "claude.ai", "anthropic.com",
        "rutracker.org", "rutracker.net", "flibusta.is", "nnmclub.to", "kinozal.tv",
        "notion.so", "canva.com", "torproject.org", "protonmail.com", "proton.me",
        "meduza.io", "bbc.com", "dw.com", "svoboda.org",
        "youtube.com", "googlevideo.com", "ytimg.com", "youtu.be"
    )

    /**
     * Injects anti-DPI / anti-TSPU settings, urltest failover outbound, DoH DNS,
     * and IPv6 blackhole defense rules into a raw sing-box JSON configuration.
     * Sanitizes deprecated geosite/geoip rules for 100% sing-box 1.12.0+ compatibility.
     *
     * @param jsonString raw JSON config string from API, 3x-ui, or link parser
     * @param udpState optional current UDP health state from UdpProber
     * @return modified JSON config string
     */
    fun inject(jsonString: String, udpState: UdpProber.UdpState? = null): String {
        return try {
            val json = JSONObject(jsonString)
            sanitizeDeprecatedFields(json)
            injectLogConfig(json)
            injectInbounds(json)
            injectOutbounds(json, udpState)
            injectDns(json)
            injectRouteRules(json)
            json.toString(2)

        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ConfigInjector", "Failed to inject anti-DPI settings: ${e.message}", e)
            jsonString
        }
    }

    /**
     * Removes deprecated `geosite` and `geoip` fields from DNS and Route rules
     * to ensure 100% compatibility with sing-box 1.12.0+ (where geosite/geoip DBs are removed).
     */
    private fun sanitizeDeprecatedFields(json: JSONObject) {
        val isAdBlockEnabled = io.nekohasekai.sfa.database.Settings.adblockEnabled
        val isRoutingAll = io.nekohasekai.sfa.database.Settings.routingMode == io.nekohasekai.sfa.database.Settings.ROUTING_MODE_ALL

        // 1. Sanitize DNS rules and deprecated fakeip
        val dns = json.optJSONObject("dns")
        if (dns != null) {
            dns.remove("fakeip")
            val rules = dns.optJSONArray("rules")
            if (rules != null) {
                val cleanedRules = JSONArray()
                for (i in 0 until rules.length()) {
                    val rule = rules.optJSONObject(i) ?: continue
                    rule.remove("geosite")
                    rule.remove("geoip")
                    val ruleSet = rule.optJSONArray("rule_set")
                    val isAdRule = ruleSet != null && (0 until ruleSet.length()).any { ruleSet.optString(it).contains("ads") }
                    if (!isAdBlockEnabled && isAdRule) continue

                    val isRuRule = ruleSet != null && (0 until ruleSet.length()).any { ruleSet.optString(it).contains("-ru") }
                    if (isRoutingAll && isRuRule) continue

                    // Keep rule if it still has valid conditions or actions
                    if (rule.length() > 0 && (rule.has("server") || rule.has("action") || rule.has("outbound") || rule.has("domain") || rule.has("domain_suffix") || rule.has("domain_keyword") || rule.has("rule_set"))) {
                        cleanedRules.put(rule)
                    }
                }
                dns.put("rules", cleanedRules)
            }
        }

        // 2. Sanitize Route rules
        val route = json.optJSONObject("route")
        if (route != null) {
            val rules = route.optJSONArray("rules")
            if (rules != null) {
                val cleanedRules = JSONArray()
                for (i in 0 until rules.length()) {
                    val rule = rules.optJSONObject(i) ?: continue
                    rule.remove("geosite")
                    rule.remove("geoip")
                    val ruleSet = rule.optJSONArray("rule_set")
                    val isAdRule = ruleSet != null && (0 until ruleSet.length()).any { ruleSet.optString(it).contains("ads") }
                    if (!isAdBlockEnabled && isAdRule) continue

                    val isRuRule = ruleSet != null && (0 until ruleSet.length()).any { ruleSet.optString(it).contains("-ru") }
                    if (isRoutingAll && isRuRule) continue

                    if (rule.length() > 0 && (rule.has("action") || rule.has("outbound") || rule.has("domain") || rule.has("domain_suffix") || rule.has("ip_cidr") || rule.has("ip_is_private") || rule.has("protocol") || rule.has("rule_set") || rule.has("ip_version"))) {
                        cleanedRules.put(rule)
                    }
                }
                route.put("rules", cleanedRules)
            }
        }
    }

    private fun injectInbounds(json: JSONObject) {
        val inbounds = json.optJSONArray("inbounds") ?: return
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("type") == "tun") {
                // Force userspace gVisor network stack on Android.
                // Prevents TCP loopback NAT routing drops on Android kernel.
                inbound.put("stack", "gvisor")
                inbound.put("auto_route", true)
                inbound.put("strict_route", false)
            }
        }
    }

    private fun injectOutbounds(json: JSONObject, udpState: UdpProber.UdpState?) {
        val rawOutbounds = json.optJSONArray("outbounds") ?: JSONArray().also { json.put("outbounds", it) }
        val sanitizedOutbounds = JSONArray()
        val proxyTags = mutableListOf<String>()
        val supportedTransports = setOf("http", "ws", "grpc", "httpupgrade", "quic")

        for (i in 0 until rawOutbounds.length()) {
            val outbound = rawOutbounds.optJSONObject(i) ?: continue
            val type = outbound.optString("type")
            val tag = outbound.optString("tag")

            // Filter out unsupported V2Ray transports (e.g. xhttp) that crash sing-box core at decode stage
            val transport = outbound.optJSONObject("transport")
            if (transport != null) {
                val transportType = transport.optString("type")
                if (transportType.isNotEmpty() && transportType !in supportedTransports) {
                    continue
                }
            }

            sanitizedOutbounds.put(outbound)

            // Skip helper outbounds from proxyTags
            if (type == "direct" || type == "block" || type == "dns" || type == "urltest" || type == "selector") {
                continue
            }

            // 1. TLS uTLS Chrome fingerprinting (ONLY for TCP-based TLS, NOT Hysteria 2 / QUIC / NaiveProxy!)
            if (type != "hysteria" && type != "hysteria2" && type != "tuic" && type != "naive") {
                if (outbound.has("tls") || type == "vless" || type == "vmess" || type == "trojan") {
                    val tls = outbound.optJSONObject("tls") ?: JSONObject().also { outbound.put("tls", it) }
                    tls.put("enabled", true)

                    val utls = tls.optJSONObject("utls") ?: JSONObject().also { tls.put("utls", it) }
                    utls.put("enabled", true)
                    utls.put("fingerprint", "chrome")
                }
            }

            // 2. SMUX Multiplexing (for TCP-based proxies)
            // TODO: Re-enable SMUX for supported TCP outbounds once compatible with XTLS Vision / Reality
            /*
            if (type == "vless" || type == "vmess" || type == "trojan" || type == "shadowsocks") {
                val multiplex = outbound.optJSONObject("multiplex") ?: JSONObject().also { outbound.put("multiplex", it) }
                multiplex.put("enabled", true)
                multiplex.put("protocol", "smux")
                multiplex.put("max_connections", 1)
                multiplex.put("padding", true)
            }
            */

            if (tag.isNotEmpty()) {
                proxyTags.add(tag)
            }
        }

        json.put("outbounds", sanitizedOutbounds)
        val outbounds = sanitizedOutbounds

        // 3. Freeze Detector & Failover (urltest & selector)
        if (proxyTags.isNotEmpty()) {
            val context: android.content.Context? = try {
                io.nekohasekai.sfa.Application.application
            } catch (e: Exception) {
                null
            }
            val sortedTags = if (context != null) {
                NetworkPriorityAdapter.sortProxyTags(proxyTags, context, udpState ?: UdpProber.currentState)
            } else {
                proxyTags
            }

            val urlTestOutbound = JSONObject().apply {
                put("type", "urltest")
                put("tag", "auto")
                put("outbounds", JSONArray(sortedTags))
                put("url", "http://cp.cloudflare.com/generate_204")
                put("interval", "10m")
                put("tolerance", 300)
                put("idle_timeout", "30m")
            }

            val selectorTags = mutableListOf("auto").apply { addAll(sortedTags) }
            val userTag = io.nekohasekai.sfa.database.Settings.selectedOutboundTag
            val defaultTag = if (userTag.isNotEmpty() && selectorTags.contains(userTag)) userTag else "auto"

            val selectorOutbound = JSONObject().apply {
                put("type", "selector")
                put("tag", "proxy")
                put("outbounds", JSONArray(selectorTags))
                put("default", defaultTag)
            }

            // Remove any existing outbound with tag "proxy" or "auto"
            val newOutbounds = JSONArray()
            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                val tag = ob.optString("tag")
                if (tag != "proxy" && tag != "auto") {
                    newOutbounds.put(ob)
                }
            }

            newOutbounds.put(urlTestOutbound)
            newOutbounds.put(selectorOutbound)
            json.put("outbounds", newOutbounds)
        } else {
            throw IllegalStateException("Сервер временно не вернул доступные узлы. Пожалуйста, обновите профиль.")
        }
    }

    private fun injectDns(json: JSONObject) {
        val dns = json.optJSONObject("dns") ?: JSONObject().also { json.put("dns", it) }
        val servers = dns.optJSONArray("servers") ?: JSONArray().also { dns.put("servers", it) }

        var hasDoh = false
        for (i in 0 until servers.length()) {
            val server = servers.optJSONObject(i) ?: continue
            val type = server.optString("type")
            val address = server.optString("server")
            if (type == "https" || address.contains("cloudflare") || address.contains("google")) {
                hasDoh = true
                break
            }
        }

        if (!hasDoh) {
            val dohServer = JSONObject().apply {
                put("tag", "doh-dns")
                put("type", "https")
                put("server", "1.1.1.1")
                put("path", "/dns-query")
                put("detour", "proxy")
            }
            servers.put(dohServer)
        }

        val rules = dns.optJSONArray("rules") ?: JSONArray().also { dns.put("rules", it) }
        val routingMode = io.nekohasekai.sfa.database.Settings.routingMode
        if (routingMode == io.nekohasekai.sfa.database.Settings.ROUTING_MODE_BLOCKED) {
            val targetDns = if (hasDoh) "remote-dns" else "doh-dns"
            val blockedDnsRule = JSONObject().apply {
                put("domain_suffix", JSONArray(BLOCKED_DOMAINS))
                put("server", targetDns)
            }
            rules.put(blockedDnsRule)
        }
    }

    private fun injectRouteRules(json: JSONObject) {
        val route = json.optJSONObject("route") ?: JSONObject().also { json.put("route", it) }
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

        var hasIpv6Reject = false
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val ipVersion = rule.optInt("ip_version", 0)
            val action = rule.optString("action")
            val outbound = rule.optString("outbound")
            if (ipVersion == 6 && (action == "reject" || outbound == "block")) {
                hasIpv6Reject = true
                break
            }
        }

        if (!hasIpv6Reject) {
            val ipv6Rule = JSONObject().apply {
                put("ip_version", 6)
                put("action", "reject")
            }
            val newRules = JSONArray()
            newRules.put(ipv6Rule)
            for (i in 0 until rules.length()) {
                newRules.put(rules.get(i))
            }
            route.put("rules", newRules)
        }

        // Add ICMP direct rule to avoid warnings and enable system ping checks
        var hasIcmpDirect = false
        val currentRules = route.optJSONArray("rules") ?: JSONArray()
        for (i in 0 until currentRules.length()) {
            val rule = currentRules.optJSONObject(i) ?: continue
            val network = rule.optString("network")
            val outbound = rule.optString("outbound")
            if (network == "icmp" && (outbound == "direct" || outbound == "bypass")) {
                hasIcmpDirect = true
                break
            }
        }

        if (!hasIcmpDirect) {
            val icmpRule = JSONObject().apply {
                put("network", "icmp")
                put("outbound", "direct")
            }
            currentRules.put(icmpRule)
        }

        // Apply routing mode
        val routingMode = io.nekohasekai.sfa.database.Settings.routingMode
        if (routingMode == io.nekohasekai.sfa.database.Settings.ROUTING_MODE_BLOCKED) {
            route.put("final", "direct")
            val blockedRule = JSONObject().apply {
                put("domain_suffix", JSONArray(BLOCKED_DOMAINS))
                put("outbound", "proxy")
            }
            currentRules.put(blockedRule)
        } else {
            route.put("final", "proxy")
        }
    }

    private fun injectLogConfig(json: JSONObject) {
        val log = json.optJSONObject("log") ?: JSONObject().also { json.put("log", it) }
        if (!log.has("level") || log.optString("level") == "panic" || log.optString("level") == "warn") {
            log.put("level", "info")
        }
        log.put("disabled", false)
        log.put("timestamp", true)
    }
}


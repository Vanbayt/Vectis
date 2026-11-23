package io.nekohasekai.sfa.compose.screen.protocol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OutboundGroup
import io.nekohasekai.sfa.compose.model.toList
import io.nekohasekai.sfa.database.ProfileManager
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.utils.CommandClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

data class ProtocolItemUi(
    val tag: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val countryFlag: String,
    val protocolBadge: String,
    val badgeColorType: String = "primary", // primary, warning, success
    val isAuto: Boolean,
    val delayMs: Int = 0
)

data class ProtocolSelectionUiState(
    val selectedTag: String = "auto",
    val items: List<ProtocolItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val isTestingPing: Boolean = false
)

class ProtocolSelectionViewModel : ViewModel(), CommandClient.Handler {

    private val _uiState = MutableStateFlow(ProtocolSelectionUiState())
    val uiState: StateFlow<ProtocolSelectionUiState> = _uiState.asStateFlow()

    private var commandClient: CommandClient? = null

    fun connect() {
        disconnect()

        // 1. Dynamic load of outbounds from active profile JSON
        loadFallbackItems()

        // 2. Async connection to CommandClient for live ping & outbound updates when VPN is running
        viewModelScope.launch(Dispatchers.IO) {
            try {
                commandClient = CommandClient(
                    viewModelScope,
                    CommandClient.ConnectionType.Groups,
                    this@ProtocolSelectionViewModel
                )
                commandClient?.connect()
                runUrlTest()
            } catch (e: Exception) {
                // Ignore if service is stopped
            }
        }
    }

    fun disconnect() {
        runCatching { commandClient?.disconnect() }
        commandClient = null
    }

    fun runUrlTest() {
        _uiState.value = _uiState.value.copy(isTestingPing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Libbox.newStandaloneCommandClient().urlTest("proxy")
                Libbox.newStandaloneCommandClient().urlTest("auto")
            } catch (e: Exception) {
                loadFallbackItems()
            } finally {
                delay(1500)
                _uiState.value = _uiState.value.copy(isTestingPing = false)
            }
        }
    }

    override fun updateGroups(newGroups: MutableList<OutboundGroup>) {
        val proxyGroup = newGroups.find { it.tag == "proxy" } ?: newGroups.firstOrNull()
        val autoGroup = newGroups.find { it.tag == "auto" }
        if (proxyGroup != null) {
            val selected = Settings.selectedOutboundTag.ifEmpty { proxyGroup.selected.ifEmpty { "auto" } }
            val realSelectedTag = if (proxyGroup.selected == "auto" || proxyGroup.selected.isEmpty()) {
                autoGroup?.selected ?: proxyGroup.selected
            } else {
                proxyGroup.selected
            }

            io.nekohasekai.sfa.network.AppLogCollector.appendLog("VectisHealth", "[AutoSelector] Sing-Box proxy selected: '${proxyGroup.selected}' | Auto group active node: '${autoGroup?.selected}' | Active UI selection: '$selected'")

            val context = io.nekohasekai.sfa.Application.application
            val formattedLoc = io.nekohasekai.sfa.utils.LocationLocalizer.formatLocation(realSelectedTag)
            val proto = when {
                realSelectedTag.contains("hysteria", ignoreCase = true) || realSelectedTag.contains("hy2", ignoreCase = true) || realSelectedTag.endsWith("-8") || realSelectedTag.endsWith("-2") -> "Hysteria 2 UDP"
                realSelectedTag.contains("anytls", ignoreCase = true) -> "AnyTLS (TCP)"
                realSelectedTag.contains("naive", ignoreCase = true) -> "NaiveProxy"
                realSelectedTag.contains("tuic", ignoreCase = true) -> "TUIC v5"
                realSelectedTag.contains("grpc", ignoreCase = true) || realSelectedTag.endsWith("-5") || realSelectedTag.endsWith("-3") -> "VLESS gRPC"
                realSelectedTag.contains("httpupgrade", ignoreCase = true) || realSelectedTag.endsWith("-4") -> "VLESS HTTPUpgrade"
                else -> "VLESS Reality TCP"
            }
            val activeOutboundName = if (realSelectedTag.isBlank()) {
                context.getString(io.nekohasekai.sfa.R.string.protocol_selecting)
            } else {
                "$formattedLoc $proto"
            }

            val rawItems = proxyGroup.items.toList()
            val uiItems = mutableListOf<ProtocolItemUi>()

            // 1. Auto item
            uiItems.add(
                ProtocolItemUi(
                    tag = "auto",
                    title = context.getString(io.nekohasekai.sfa.R.string.protocol_auto_title),
                    subtitle = context.getString(io.nekohasekai.sfa.R.string.protocol_auto_subtitle, activeOutboundName),
                    description = context.getString(io.nekohasekai.sfa.R.string.protocol_auto_desc),
                    countryFlag = "",
                    protocolBadge = context.getString(io.nekohasekai.sfa.R.string.protocol_badge_smart_routing),
                    badgeColorType = "primary",
                    isAuto = true
                )
            )

            // 2. Individual server outbounds
            rawItems.forEach { item ->
                if (item.tag != "auto" && item.tag != "proxy") {
                    val formatted = formatOutboundItem(item.tag, item.urlTestDelay)
                    uiItems.add(formatted)
                }
            }

            _uiState.value = _uiState.value.copy(
                selectedTag = selected,
                items = uiItems,
                isLoading = false
            )
        }
    }

    fun selectOutbound(tag: String) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Settings.selectedOutboundTag = tag
                Libbox.newStandaloneCommandClient().selectOutbound("proxy", tag)
            } catch (_: Exception) {
            }
            // Auto-reconnect / restart VPN if active
            io.nekohasekai.sfa.bg.BoxService.start()
        }
    }

    private fun loadFallbackItems() {
        val context = io.nekohasekai.sfa.Application.application
        val uiItems = mutableListOf<ProtocolItemUi>()

        // Auto item
        uiItems.add(
            ProtocolItemUi(
                tag = "auto",
                title = context.getString(io.nekohasekai.sfa.R.string.protocol_auto_title),
                subtitle = context.getString(io.nekohasekai.sfa.R.string.protocol_auto_subtitle_fallback),
                description = context.getString(io.nekohasekai.sfa.R.string.protocol_auto_desc),
                countryFlag = "",
                protocolBadge = context.getString(io.nekohasekai.sfa.R.string.protocol_badge_smart_routing),
                badgeColorType = "primary",
                isAuto = true
            )
        )

        val endpointsToProbe = mutableListOf<io.nekohasekai.sfa.network.PreConnectPingManager.ServerEndpoint>()
        val parsedOutboundTags = mutableListOf<String>()

        try {
            val selectedId = Settings.selectedProfile
            val profile = runBlocking { ProfileManager.get(selectedId) }
            if (profile != null && profile.typed.path.isNotEmpty()) {
                val file = File(profile.typed.path)
                if (file.exists()) {
                    val jsonStr = file.readText()
                    val jsonObj = JSONObject(jsonStr)
                    val obs = jsonObj.optJSONArray("outbounds")
                    if (obs != null) {
                        val selectorTags = mutableListOf<String>()
                        val outboundMap = mutableMapOf<String, JSONObject>()
                        for (i in 0 until obs.length()) {
                            val ob = obs.optJSONObject(i) ?: continue
                            val tag = ob.optString("tag")
                            val type = ob.optString("type")
                            outboundMap[tag] = ob
                            if (tag == "proxy" && type == "selector") {
                                val outboundsArr = ob.optJSONArray("outbounds")
                                if (outboundsArr != null) {
                                    for (j in 0 until outboundsArr.length()) {
                                        val sTag = outboundsArr.optString(j)
                                        if (sTag.isNotBlank() && sTag != "auto" && sTag != "proxy") {
                                            selectorTags.add(sTag)
                                        }
                                    }
                                }
                            }
                        }

                        val targetTags = if (selectorTags.isNotEmpty()) selectorTags else outboundMap.keys.toList()
                        for (tag in targetTags) {
                            val ob = outboundMap[tag] ?: continue
                            val type = ob.optString("type")
                            if (type == "vless" || type == "hysteria2" || type == "hysteria" || type == "tuic" || type == "anytls" || type == "naive" || type == "shadowsocks") {
                                if (!tag.startsWith("shadowtls-sub-") && !tag.contains("-ss2022-") && !tag.contains("-reality-12928")) {
                                    parsedOutboundTags.add(tag)
                                    val host = ob.optString("server")
                                    val port = ob.optInt("server_port", 443)
                                    if (host.isNotEmpty() && port > 0) {
                                        val isUdp = (type == "hysteria2" || type == "hysteria" || type == "tuic")
                                        endpointsToProbe.add(
                                            io.nekohasekai.sfa.network.PreConnectPingManager.ServerEndpoint(tag, host, port, isUdp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // Dynamically probe socket latency for all parsed endpoints
        if (endpointsToProbe.isNotEmpty()) {
            io.nekohasekai.sfa.network.PreConnectPingManager.probeAll(endpointsToProbe)
        }

        val tagsToUse = if (parsedOutboundTags.isNotEmpty()) {
            parsedOutboundTags
        } else {
            listOf(
                "proxy-sandbox-hy2-8443",
                "proxy-sandbox-reality-8447",
                "proxy-sandbox-naive-443",
                "proxy-sandbox-tuic-9443",
                "proxy-sandbox-shadowtls-8444"
            )
        }

        tagsToUse.forEach { tag ->
            if (tag != "auto" && tag != "proxy") {
                uiItems.add(formatOutboundItem(tag, 0))
            }
        }

        val currentSelected = Settings.selectedOutboundTag.let { if (it.isBlank()) "auto" else it }

        _uiState.value = ProtocolSelectionUiState(
            selectedTag = currentSelected,
            items = uiItems,
            isLoading = false
        )
    }

    private fun formatOutboundItem(tag: String, delay: Int): ProtocolItemUi {
        val context = io.nekohasekai.sfa.Application.application
        val effectiveDelay = if (delay > 0) delay else io.nekohasekai.sfa.network.PreConnectPingManager.getPingForTag(tag)

        val locInfo = io.nekohasekai.sfa.utils.LocationLocalizer.getLocationInfo(tag)
        val countryFlag = locInfo.flag

        val isHysteria = tag.contains("hysteria", ignoreCase = true) || tag.contains("hy2", ignoreCase = true) || tag.endsWith("-8") || tag.endsWith("-2") || tag.endsWith("-9")
        val isGrpc = tag.contains("grpc", ignoreCase = true) || tag.endsWith("-5") || tag.endsWith("-3")
        val isHttpUpgrade = tag.contains("httpupgrade", ignoreCase = true) || tag.endsWith("-4")
        val isAnyTls = tag.contains("anytls", ignoreCase = true)
        val isNaive = tag.contains("naive", ignoreCase = true)
        val isTuic = tag.contains("tuic", ignoreCase = true)
        val isShadowTls = tag.contains("shadowtls", ignoreCase = true) || tag.contains("ss2022", ignoreCase = true)

        val protocolName = when {
            isHysteria -> "Hysteria 2 (UDP)"
            isAnyTls -> "AnyTLS (TCP)"
            isNaive -> "NaiveProxy (Chromium)"
            isTuic -> "TUIC v5 (BBR)"
            isShadowTls -> "ShadowTLS v3"
            isGrpc -> "VLESS gRPC"
            isHttpUpgrade -> "VLESS HTTPUpgrade"
            else -> "VLESS Reality (TCP)"
        }

        val protocolBadge = when {
            isHysteria -> context.getString(io.nekohasekai.sfa.R.string.protocol_badge_mobile)
            isAnyTls -> "0-RTT Pool"
            isNaive -> "Анти-ТСПУ"
            isTuic -> "BBR Speed"
            isShadowTls -> "Скрытный TLS"
            isGrpc -> context.getString(io.nekohasekai.sfa.R.string.protocol_badge_dpi)
            isHttpUpgrade -> context.getString(io.nekohasekai.sfa.R.string.protocol_badge_dpi)
            else -> context.getString(io.nekohasekai.sfa.R.string.protocol_badge_speed)
        }

        val badgeColorType = when {
            isHysteria -> "warning"
            isAnyTls -> "primary"
            isNaive -> "success"
            isTuic -> "warning"
            isGrpc -> "success"
            else -> "primary"
        }

        val description = when {
            isHysteria -> context.getString(io.nekohasekai.sfa.R.string.protocol_desc_hysteria)
            isAnyTls -> "AnyTLS: маскировка с пре-разогретыми сессиями и рандомизированным паддингом"
            isNaive -> "NaiveProxy: туннель HTTP/2 CONNECT через Chromium Cronet стек"
            isTuic -> "TUIC v5: высокоскоростной протокол поверх QUIC с контроллером перегрузки BBR"
            isShadowTls -> "ShadowTLS v3: двойное сокрытие Shadowsocks-2022 под TLS"
            isGrpc -> context.getString(io.nekohasekai.sfa.R.string.protocol_desc_grpc)
            else -> context.getString(io.nekohasekai.sfa.R.string.protocol_desc_vless)
        }

        val formattedLoc = io.nekohasekai.sfa.utils.LocationLocalizer.formatLocation(tag)
        val title = protocolName
        val subtitle = "$formattedLoc • $protocolName"

        return ProtocolItemUi(
            tag = tag,
            title = title,
            subtitle = subtitle,
            description = description,
            countryFlag = "",
            protocolBadge = protocolBadge,
            badgeColorType = badgeColorType,
            isAuto = false,
            delayMs = effectiveDelay
        )
    }
}

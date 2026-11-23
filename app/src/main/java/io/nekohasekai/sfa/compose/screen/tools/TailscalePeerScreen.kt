package io.nekohasekai.sfa.compose.screen.tools

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.LineChart
import io.nekohasekai.sfa.ktx.clipboardText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailscalePeerScreen(
    navController: NavController,
    viewModel: TailscaleStatusViewModel,
    endpointTag: String,
    peerId: String,
) {
    val state by viewModel.uiState.collectAsState()
    val peer = viewModel.peer(endpointTag, peerId)
    val endpoint = viewModel.endpoint(endpointTag)
    val isSelf = endpoint?.selfPeer?.id == peerId
    val pingViewModel: TailscalePingViewModel = viewModel()
    val pingState by pingViewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            if (pingState.isRunning) {
                pingViewModel.stopPing()
            }
        }
    }

    if (peer == null) {
        LaunchedEffect(Unit) {
            navController.navigateUp()
        }
        return
    }

    var copiedAddress by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            peer.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (peer.online) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                            )
                            Text(
                                if (peer.online) {
                                    stringResource(R.string.tailscale_connected)
                                } else {
                                    stringResource(R.string.tailscale_not_connected)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            // Network section (self peer only): network name + logout
            val networkName = endpoint?.networkName
            val showLogout = isSelf && endpoint?.backendState == "Running" && endpoint?.keyAuth == false
            if (isSelf && (!networkName.isNullOrEmpty() || showLogout)) {
                SectionHeader(stringResource(R.string.tailscale_network))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        if (!networkName.isNullOrEmpty()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AddressRow(
                                    address = networkName,
                                    label = stringResource(R.string.tailscale_network),
                                    copied = copiedAddress,
                                    onCopy = { copiedAddress = it },
                                )
                            }
                        }
                        if (showLogout) {
                            if (!networkName.isNullOrEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                            ListItem(
                                headlineContent = {
                                    Text(
                                        stringResource(R.string.tailscale_logout),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.logout(endpointTag)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }

            // Tailscale Addresses section
            SectionHeader(stringResource(R.string.tailscale_addresses))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (peer.dnsName.isNotEmpty()) {
                        AddressRow(
                            address = Libbox.formatFQDN(peer.dnsName),
                            label = stringResource(R.string.tailscale_magic_dns),
                            copied = copiedAddress,
                            onCopy = { copiedAddress = it },
                        )
                    }
                    if (peer.hostName.isNotEmpty()) {
                        AddressRow(
                            address = peer.hostName,
                            label = stringResource(R.string.tailscale_hostname),
                            copied = copiedAddress,
                            onCopy = { copiedAddress = it },
                        )
                    }
                    for (ip in peer.tailscaleIPs) {
                        AddressRow(
                            address = ip,
                            label = if (ip.contains(":")) {
                                stringResource(R.string.tailscale_ipv6)
                            } else {
                                stringResource(R.string.tailscale_ipv4)
                            },
                            copied = copiedAddress,
                            onCopy = { copiedAddress = it },
                        )
                    }
                }
            }

            // Ping section (not for self peer)
            if (!isSelf && peer.online && peer.tailscaleIPs.isNotEmpty()) {
                val peerIP = peer.tailscaleIPs.first()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.tailscale_ping),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (pingState.isRunning) {
                                pingViewModel.stopPing()
                            } else {
                                pingViewModel.startPing(endpointTag, peerIP)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(width = 44.dp, height = 36.dp),
                    ) {
                        Icon(
                            imageVector = if (pingState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (pingState.isRunning) {
                                stringResource(R.string.tailscale_ping_stop)
                            } else {
                                stringResource(R.string.tailscale_ping_start)
                            },
                            modifier = Modifier.size(20.dp),
                            tint = if (pingState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        if (pingState.hasResult) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (pingState.isDirect) {
                                    Text(
                                        text = "\u2192 ",
                                        color = Color(0xFF2E7D32),
                                    )
                                    Text(
                                        text = stringResource(R.string.tailscale_ping_direct),
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                } else {
                                    Text(
                                        text = "\u21BB ",
                                        color = Color(0xFFF57C00),
                                    )
                                    Text(
                                        text = stringResource(R.string.tailscale_ping_derp),
                                        color = Color(0xFFF57C00),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${pingState.latencyMs.toInt()} ms",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFeatureSettings = "tnum",
                                    ),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (pingState.isRunning && pingState.latencyHistory.size > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LineChart(
                                        data = pingState.latencyHistory,
                                        lineColor = if (pingState.isDirect) {
                                            Color(0xFF2E7D32)
                                        } else {
                                            Color(0xFF2196F3)
                                        },
                                        animate = false,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val maxMs = (
                                        (
                                            pingState.latencyHistory.maxOrNull()
                                                ?: 1f
                                            ) * 1.2f
                                        ).toInt().coerceAtLeast(1)
                                    Column(
                                        modifier = Modifier.height(80.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "${maxMs}ms",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "${maxMs * 2 / 3}ms",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "${maxMs / 3}ms",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "0ms",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No data",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // Details section
            SectionHeader(stringResource(R.string.tailscale_details))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        peer.expired -> {
                            DetailRow(
                                label = stringResource(R.string.tailscale_key_expiry),
                                value = stringResource(R.string.tailscale_expired),
                                valueColor = MaterialTheme.colorScheme.error,
                            )
                        }
                        peer.keyExpiry > 0 -> {
                            val expiryText = DateUtils.getRelativeTimeSpanString(
                                peer.keyExpiry * 1000,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                0,
                            ).toString()
                            DetailRow(
                                label = stringResource(R.string.tailscale_key_expiry),
                                value = expiryText,
                            )
                        }
                        else -> {
                            DetailRow(
                                label = stringResource(R.string.tailscale_key_expiry),
                                value = stringResource(R.string.disabled),
                                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (peer.os.isNotEmpty()) {
                        DetailRow(
                            label = stringResource(R.string.tailscale_os),
                            value = peer.os,
                        )
                    }
                    if (!peer.online && peer.lastSeen > 0) {
                        val lastSeenText = DateUtils.getRelativeTimeSpanString(
                            peer.lastSeen * 1000,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            0,
                        ).toString()
                        DetailRow(
                            label = stringResource(R.string.tailscale_last_seen),
                            value = lastSeenText,
                        )
                    }
                    if (peer.exitNode) {
                        DetailRow(
                            label = stringResource(R.string.tailscale_exit_node),
                            value = stringResource(R.string.tailscale_active),
                        )
                    } else if (peer.exitNodeOption) {
                        DetailRow(
                            label = stringResource(R.string.tailscale_exit_node),
                            value = stringResource(R.string.tailscale_available),
                        )
                    }
                    if (peer.shareeNode) {
                        DetailRow(
                            label = stringResource(R.string.tailscale_shared_in),
                            value = stringResource(R.string.tailscale_yes),
                        )
                    }
                    if (peer.sshHostKeys.isNotEmpty()) {
                        if (!peer.online || isSelf || peer.tailscaleIPs.isEmpty()) {
                            DetailRow(
                                label = stringResource(R.string.tailscale_ssh),
                                value = stringResource(R.string.tailscale_available),
                            )
                        }
                    }
                }
            }

            if (peer.sshHostKeys.isNotEmpty() && peer.online && !isSelf && peer.tailscaleIPs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.tailscale_ssh_connect),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier
                            .clickable {
                                navController.navigate(
                                    "tools/tailscale/${android.net.Uri.encode(endpointTag)}/peer/${android.net.Uri.encode(peerId)}/ssh",
                                )
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }

    LaunchedEffect(copiedAddress) {
        if (copiedAddress != null) {
            delay(2000)
            copiedAddress = null
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontFeatureSettings = "tnum",
            ),
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AddressRow(
    address: String,
    label: String,
    copied: String?,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                address,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = {
            clipboardText = address
            onCopy(address)
        }) {
            if (copied == address) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

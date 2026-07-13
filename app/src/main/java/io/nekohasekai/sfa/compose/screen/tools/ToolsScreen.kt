package io.nekohasekai.sfa.compose.screen.tools

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.bg.CrashReportManager
import io.nekohasekai.sfa.bg.OOMReportManager
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.terminal.TailscaleSSHPresentedSession
import io.nekohasekai.sfa.compose.screen.settings.SettingTile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ToolsScreen(
    navController: NavController,
    serviceStatus: Status = Status.Stopped,
    tailscaleViewModel: TailscaleStatusViewModel,
    sshSharedViewModel: TailscaleSSHSharedViewModel,
) {
    OverrideTopBar {
        TopAppBar(
            title = { Text(stringResource(R.string.title_tools)) },
        )
    }

    val crashUnreadCount by CrashReportManager.unreadCount.collectAsState()
    val oomUnreadCount by OOMReportManager.unreadCount.collectAsState()
    val tailscaleState by tailscaleViewModel.uiState.collectAsState()

    LaunchedEffect(serviceStatus) {
        if (serviceStatus == Status.Started) {
            tailscaleViewModel.subscribe()
        } else {
            tailscaleViewModel.cancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        if (tailscaleState.endpoints.isNotEmpty()) {
            Text(
                text = stringResource(R.string.tailscale_endpoints),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                val endpoints = tailscaleState.endpoints
                endpoints.forEachIndexed { index, endpoint ->
                    var showSSHMenu by remember { mutableStateOf(false) }
                    val sshPeers = remember(endpoint) {
                        endpoint.userGroups.flatMap { it.peers }.filter { peer ->
                            peer.online && peer.sshHostKeys.isNotEmpty() &&
                                peer.tailscaleIPs.isNotEmpty() && peer.id != endpoint.selfPeer?.id
                        }
                    }
                    Box {
                        SettingTile(
                            icon = Icons.Outlined.Hub,
                            title = if (endpoints.size == 1) {
                                stringResource(R.string.tailscale)
                            } else {
                                stringResource(R.string.tailscale_with_tag, endpoint.endpointTag)
                            },
                            onClick = { navController.navigate("tools/tailscale/${Uri.encode(endpoint.endpointTag)}") }
                        )
                        DropdownMenu(
                            expanded = showSSHMenu,
                            onDismissRequest = { showSSHMenu = false },
                        ) {
                            if (sshPeers.size == 1) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tailscale_ssh_connect)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Terminal, contentDescription = null)
                                    },
                                    onClick = {
                                        showSSHMenu = false
                                        handleSSHNavigation(
                                            navController,
                                            sshSharedViewModel,
                                            sshPeers[0],
                                            endpoint.endpointTag,
                                        )
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tailscale_ssh_connect)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Terminal, contentDescription = null)
                                    },
                                    enabled = false,
                                    onClick = {},
                                )
                                sshPeers.forEach { peer ->
                                    DropdownMenuItem(
                                        text = { Text(peer.hostName) },
                                        onClick = {
                                            showSSHMenu = false
                                            handleSSHNavigation(
                                                navController,
                                                sshSharedViewModel,
                                                peer,
                                                endpoint.endpointTag,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.title_network),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )

        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        ) {
            SettingTile(
                icon = Icons.Outlined.NetworkCheck,
                title = stringResource(R.string.network_quality),
                onClick = { navController.navigate("tools/network_quality") }
            )
            SettingTile(
                icon = Icons.Outlined.NetworkCheck,
                title = stringResource(R.string.stun_test),
                onClick = { navController.navigate("tools/stun_test") }
            )
        }

        Text(
            text = stringResource(R.string.title_debug),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            SettingTile(
                icon = Icons.Outlined.BugReport,
                title = stringResource(R.string.crash_report),
                onClick = { navController.navigate("tools/crash_report") },
                badgeText = if (crashUnreadCount > 0) crashUnreadCount.toString() else null
            )
            SettingTile(
                icon = Icons.Outlined.Memory,
                title = stringResource(R.string.oom_report),
                onClick = { navController.navigate("tools/oom_report") },
                badgeText = if (oomUnreadCount > 0) oomUnreadCount.toString() else null
            )
        }
    }
}

internal fun handleSSHNavigation(
    navController: NavController,
    sshSharedViewModel: TailscaleSSHSharedViewModel,
    peer: TailscalePeerData,
    endpointTag: String,
) {
    val quickConnectPeers = Settings.tailscaleSSHQuickConnectPeers
    if (quickConnectPeers.contains(peer.stableID)) {
        val usernames = Settings.tailscaleSSHRememberedUsernames
        sshSharedViewModel.setPendingSession(
            TailscaleSSHPresentedSession(
                endpointTag = endpointTag,
                peerHostName = peer.hostName,
                peerAddress = peer.tailscaleIPs.first(),
                username = usernames[peer.stableID]?.takeIf { it.isNotBlank() } ?: "root",
                hostKeys = peer.sshHostKeys,
            ),
        )
        navController.navigate(
            "tools/tailscale/${Uri.encode(endpointTag)}/peer/${Uri.encode(peer.id)}/terminal",
        )
    } else {
        navController.navigate(
            "tools/tailscale/${Uri.encode(endpointTag)}/peer/${Uri.encode(peer.id)}/ssh",
        )
    }
}

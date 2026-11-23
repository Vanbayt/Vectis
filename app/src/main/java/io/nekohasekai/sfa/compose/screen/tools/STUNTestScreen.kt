package io.nekohasekai.sfa.compose.screen.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.constant.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun STUNTestScreen(
    navController: NavController,
    serviceStatus: Status = Status.Stopped,
    viewModel: STUNTestViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val vpnRunning = serviceStatus == Status.Started

    var showServerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vpnRunning) {
        if (!vpnRunning) {
            viewModel.onVpnDisconnected()
        }
    }

    val selectedOutboundResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("selected_outbound", state.selectedOutbound)
        ?.collectAsState()
    LaunchedEffect(selectedOutboundResult?.value) {
        selectedOutboundResult?.value?.let { viewModel.selectOutbound(it) }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (state.isRunning) {
                viewModel.cancelTest()
            }
        }
    }

    if (showServerDialog) {
        ServerEditDialog(
            currentServer = state.server,
            onServerChanged = { viewModel.updateServer(it) },
            onDismiss = { showServerDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stun_test), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.tool_configuration),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.stun_server), style = MaterialTheme.typography.bodyLarge)
                        },
                        supportingContent = {
                            Text(
                                state.server,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.clickable { showServerDialog = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    if (vpnRunning) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        OutboundPickerRow(
                            selectedOutbound = state.selectedOutbound,
                            onClick = {
                                navController.navigate(
                                    "tools/outbound_picker/${android.net.Uri.encode(state.selectedOutbound)}",
                                )
                            },
                        )
                    }
                }
            }

            if (state.isRunning) {
                Button(
                    onClick = { viewModel.cancelTest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.stun_cancel), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { viewModel.startTest(vpnRunning) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.stun_start), fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.phase >= 0) {
                Text(
                    text = stringResource(R.string.tool_results),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        ResultItem(
                            label = stringResource(R.string.stun_external_address),
                            value = state.externalAddr.ifEmpty { null },
                            isActive = state.phase == Libbox.STUNPhaseBinding.toInt(),
                            isRunning = state.isRunning,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        ResultItem(
                            label = stringResource(R.string.stun_latency),
                            value = if (state.latencyMs > 0) "${state.latencyMs} ms" else null,
                            isActive = state.phase == Libbox.STUNPhaseBinding.toInt(),
                            isRunning = state.isRunning,
                        )
                        if (state.phase == Libbox.STUNPhaseDone.toInt() && !state.natTypeSupported) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            ResultItem(
                                label = stringResource(R.string.stun_nat_type_detection),
                                value = stringResource(R.string.stun_nat_not_supported),
                                isActive = false,
                                isRunning = false,
                            )
                        } else {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            ResultItem(
                                label = stringResource(R.string.stun_nat_mapping),
                                value = if (state.natMapping > 0) Libbox.formatNATMapping(state.natMapping) else null,
                                isActive = state.phase == Libbox.STUNPhaseNATMapping.toInt(),
                                isRunning = state.isRunning,
                                valueColor = if (state.natMapping > 0) natMappingColor(state.natMapping) else null,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            ResultItem(
                                label = stringResource(R.string.stun_nat_filtering),
                                value = if (state.natFiltering > 0) Libbox.formatNATFiltering(state.natFiltering) else null,
                                isActive = state.phase == Libbox.STUNPhaseNATFiltering.toInt(),
                                isRunning = state.isRunning,
                                valueColor = if (state.natFiltering > 0) natFilteringColor(state.natFiltering) else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun natMappingColor(value: Int): Color = when (value) {
    Libbox.NATMappingEndpointIndependent.toInt() -> Color(0xFF2E7D32)
    Libbox.NATMappingAddressDependent.toInt() -> Color(0xFFF57C00)
    Libbox.NATMappingAddressAndPortDependent.toInt() -> Color(0xFFD32F2F)
    else -> Color.Unspecified
}

private fun natFilteringColor(value: Int): Color = when (value) {
    Libbox.NATFilteringEndpointIndependent.toInt() -> Color(0xFF2E7D32)
    Libbox.NATFilteringAddressDependent.toInt() -> Color(0xFFF57C00)
    Libbox.NATFilteringAddressAndPortDependent.toInt() -> Color(0xFFD32F2F)
    else -> Color.Unspecified
}

@Composable
private fun ServerEditDialog(
    currentServer: String,
    onServerChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentServer) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stun_server)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onServerChanged(text)
                onDismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

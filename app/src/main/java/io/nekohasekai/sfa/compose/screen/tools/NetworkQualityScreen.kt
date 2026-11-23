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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
fun NetworkQualityScreen(
    navController: NavController,
    serviceStatus: Status = Status.Stopped,
    viewModel: NetworkQualityViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val vpnRunning = serviceStatus == Status.Started
    val context = LocalContext.current

    var showConfigURLDialog by remember { mutableStateOf(false) }
    var showMaxRuntimeDialog by remember { mutableStateOf(false) }

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

    if (state.showMeteredWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMeteredWarning() },
            title = { Text(stringResource(R.string.network_quality_metered_title)) },
            text = { Text(stringResource(R.string.network_quality_metered_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmMeteredStart(vpnRunning) }) {
                    Text(stringResource(R.string.network_quality_metered_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMeteredWarning() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showConfigURLDialog) {
        ConfigURLDialog(
            currentURL = state.configURL,
            onURLChanged = { viewModel.updateConfigURL(it) },
            onDismiss = { showConfigURLDialog = false },
        )
    }

    if (showMaxRuntimeDialog) {
        MaxRuntimeDialog(
            currentOption = state.maxRuntime,
            onOptionSelected = {
                viewModel.setMaxRuntime(it)
                showMaxRuntimeDialog = false
            },
            onDismiss = { showMaxRuntimeDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_quality), fontWeight = FontWeight.Bold) },
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
                            Text(stringResource(R.string.network_quality_url), style = MaterialTheme.typography.bodyLarge)
                        },
                        supportingContent = {
                            Text(
                                state.configURL,
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
                        modifier = Modifier.clickable { showConfigURLDialog = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.network_quality_serial), style = MaterialTheme.typography.bodyLarge)
                        },
                        trailingContent = {
                            Switch(
                                checked = state.serial,
                                onCheckedChange = null,
                                enabled = !state.isRunning,
                            )
                        },
                        modifier = Modifier.clickable(enabled = !state.isRunning) {
                            viewModel.setSerial(!state.serial)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.network_quality_http3), style = MaterialTheme.typography.bodyLarge)
                        },
                        trailingContent = {
                            Switch(
                                checked = state.http3,
                                onCheckedChange = null,
                                enabled = !state.isRunning,
                            )
                        },
                        modifier = Modifier.clickable(enabled = !state.isRunning) {
                            viewModel.setHttp3(!state.http3)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.network_quality_max_runtime), style = MaterialTheme.typography.bodyLarge)
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(state.maxRuntime.labelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = !state.isRunning) {
                            showMaxRuntimeDialog = true
                        },
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
                    Text(stringResource(R.string.network_quality_cancel), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { viewModel.requestStartTest(context, vpnRunning) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.network_quality_start), fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.phase >= 0) {
                val phaseDownload = Libbox.NetworkQualityPhaseDownload.toInt()
                val phaseUpload = Libbox.NetworkQualityPhaseUpload.toInt()
                val downloadActive =
                    (state.isRunning && !state.serial && state.phase in phaseDownload..phaseUpload) || state.phase == phaseDownload
                val uploadActive =
                    (state.isRunning && !state.serial && state.phase in phaseDownload..phaseUpload) || state.phase == phaseUpload
                val done = state.phase == Libbox.NetworkQualityPhaseDone.toInt()

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
                            label = stringResource(R.string.network_quality_idle_latency),
                            value = if (state.idleLatencyMs > 0) "${state.idleLatencyMs} ms" else null,
                            isActive = state.phase == Libbox.NetworkQualityPhaseIdle.toInt(),
                            isRunning = state.isRunning,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        ResultItem(
                            label = stringResource(R.string.network_quality_download),
                            value = if (state.downloadCapacity > 0) Libbox.formatBitrate(state.downloadCapacity) else null,
                            isActive = downloadActive,
                            isRunning = state.isRunning,
                            accuracy = if (done) accuracyLabel(state.downloadCapacityAccuracy).first else null,
                            accuracyColor = if (done) accuracyLabel(state.downloadCapacityAccuracy).second else null,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        ResultItem(
                            label = stringResource(R.string.network_quality_download_rpm),
                            value = if (state.downloadRPM > 0) "${state.downloadRPM}" else null,
                            isActive = downloadActive,
                            isRunning = state.isRunning,
                            accuracy = if (done) accuracyLabel(state.downloadRPMAccuracy).first else null,
                            accuracyColor = if (done) accuracyLabel(state.downloadRPMAccuracy).second else null,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        ResultItem(
                            label = stringResource(R.string.network_quality_upload),
                            value = if (state.uploadCapacity > 0) Libbox.formatBitrate(state.uploadCapacity) else null,
                            isActive = uploadActive,
                            isRunning = state.isRunning,
                            accuracy = if (done) accuracyLabel(state.uploadCapacityAccuracy).first else null,
                            accuracyColor = if (done) accuracyLabel(state.uploadCapacityAccuracy).second else null,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        ResultItem(
                            label = stringResource(R.string.network_quality_upload_rpm),
                            value = if (state.uploadRPM > 0) "${state.uploadRPM}" else null,
                            isActive = uploadActive,
                            isRunning = state.isRunning,
                            accuracy = if (done) accuracyLabel(state.uploadRPMAccuracy).first else null,
                            accuracyColor = if (done) accuracyLabel(state.uploadRPMAccuracy).second else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun accuracyLabel(value: Int): Pair<String, Color> = when (value) {
    Libbox.NetworkQualityAccuracyHigh -> stringResource(R.string.network_quality_confidence_high) to Color(0xFF2E7D32)
    Libbox.NetworkQualityAccuracyMedium -> stringResource(R.string.network_quality_confidence_medium) to Color(0xFFF57C00)
    else -> stringResource(R.string.network_quality_confidence_low) to MaterialTheme.colorScheme.error
}

@Composable
private fun ConfigURLDialog(
    currentURL: String,
    onURLChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentURL) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_quality_url)) },
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
                onURLChanged(text)
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

@Composable
private fun MaxRuntimeDialog(
    currentOption: MaxRuntimeOption,
    onOptionSelected: (MaxRuntimeOption) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_quality_max_runtime)) },
        text = {
            Column {
                MaxRuntimeOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentOption == option,
                            onClick = null,
                        )
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

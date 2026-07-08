package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val isConnected = serviceStatus == Status.Started || serviceStatus == Status.Starting
    val isConnecting = serviceStatus == Status.Starting
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()

    // Подписываемся на ошибки из ViewModel
    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    var uptimeString by remember { mutableStateOf("00:00") }
    LaunchedEffect(isConnected, state.serviceStartTime) {
        if (isConnected && state.serviceStartTime != null) {
            while (isActive) {
                val nowMs = System.currentTimeMillis()
                val startMs = if (state.serviceStartTime!! > 1000000000000L) state.serviceStartTime!! else state.serviceStartTime!! * 1000
                val diff = (nowMs - startMs) / 1000
                if (diff >= 0) {
                    val hours = diff / 3600
                    val mins = (diff % 3600) / 60
                    val secs = diff % 60
                    uptimeString = if (hours > 0) {
                        String.format("%02d:%02d:%02d", hours, mins, secs)
                    } else {
                        String.format("%02d:%02d", mins, secs)
                    }
                }
                delay(1000)
            }
        } else {
            uptimeString = "00:00"
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            // Header Row replacing TopAppBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vectis",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = { /* TODO: Navigate to Profile */ }) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp))
                    }
                }
            }
            
            // Cards Grid area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Toggles Row
                val killSwitchEnabled = remember { mutableStateOf(false) }
                val splitTunnelEnabled = remember { mutableStateOf(false) }
                val autoConnectEnabled = remember { mutableStateOf(false) }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = killSwitchEnabled.value,
                            onClick = { killSwitchEnabled.value = !killSwitchEnabled.value },
                            label = { Text("Kill Switch") },
                            leadingIcon = { Icon(Icons.Rounded.GppBad, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = splitTunnelEnabled.value,
                            onClick = { splitTunnelEnabled.value = !splitTunnelEnabled.value },
                            label = { Text("Split Tunnel") },
                            leadingIcon = { Icon(Icons.Rounded.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = autoConnectEnabled.value,
                            onClick = { autoConnectEnabled.value = !autoConnectEnabled.value },
                            label = { Text("Auto-connect") },
                            leadingIcon = { Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                // Traffic Stats
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        title = "Download",
                        value = state.downlink,
                        icon = Icons.Rounded.ArrowDownward
                    )
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        title = "Upload",
                        value = state.uplink,
                        icon = Icons.Rounded.ArrowUpward
                    )
                }

                // Location and Protocol
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        title = "Location",
                        value = state.location,
                        icon = Icons.Rounded.Place
                    )
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        title = "Protocol",
                        value = state.protocol,
                        icon = Icons.Rounded.Lock
                    )
                }
                
                // Account/Subscription Card
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Subscription",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Premium Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "14 days left",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    DataUsageCard()
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Slider pinned at the bottom
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                SwipeToConnectSlider(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    onConnect = {
                        if (serviceStatus == Status.Stopped) {
                            viewModel.connectVpn()
                        }
                    },
                    onDisconnect = {
                        if (serviceStatus == Status.Started || serviceStatus == Status.Starting) viewModel.toggleService()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Session Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Uptime: $uptimeString", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Text("  •  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ping: ${state.ping}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState, 
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SwipeToConnectSlider(
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var lastHapticPosition by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    
    val width = 280.dp
    val height = 80.dp
    val thumbSize = 64.dp
    val padding = 8.dp
    
    val maxSwipePx = with(LocalDensity.current) { (width - thumbSize - (padding * 2)).toPx() }

    LaunchedEffect(isConnected, isConnecting) {
        if (isConnected) {
            offsetX.animateTo(maxSwipePx)
        } else if (!isConnecting) {
            offsetX.animateTo(0f)
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "sliderBackground"
    )

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isConnected && !isConnecting) { onDisconnect() }, // Нажатие для отключения
        contentAlignment = Alignment.CenterStart
    ) {
        // Centered Text
        val textAlpha = 1f - (offsetX.value / maxSwipePx * 1.5f).coerceIn(0f, 1f)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isConnected) "Tap or swipe to disconnect" else "Swipe to connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(textAlpha * 0.5f)
                )
            }
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(start = padding)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface)
                .pointerInput(isConnecting, isConnected) {
                    if (isConnecting) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            lastHapticPosition = 0f
                            coroutineScope.launch {
                                if (isConnected) {
                                    // Swipe back to disconnect logic
                                    if (offsetX.value < maxSwipePx * 0.8f) {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDisconnect()
                                    } else {
                                        offsetX.animateTo(maxSwipePx, spring())
                                    }
                                } else {
                                    // Swipe forward to connect logic
                                    if (offsetX.value > maxSwipePx * 0.7f) {
                                        offsetX.animateTo(maxSwipePx, spring())
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConnect()
                                    } else {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f))
                                        onDisconnect()
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            lastHapticPosition = 0f
                            coroutineScope.launch {
                                offsetX.animateTo(if (isConnected) maxSwipePx else 0f, spring(dampingRatio = 0.5f, stiffness = 200f))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Add resistance
                                val resistance = if (isConnected) {
                                    (offsetX.value / maxSwipePx * 0.6f) + 0.4f
                                } else {
                                    1f - (offsetX.value / maxSwipePx * 0.6f)
                                }
                                offsetX.snapTo((offsetX.value + dragAmount * resistance).coerceIn(0f, maxSwipePx))
                                
                                if (abs(offsetX.value - lastHapticPosition) > 40f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticPosition = offsetX.value
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Rounded.PowerSettingsNew else Icons.Rounded.Lock,
                contentDescription = "Swipe thumb",
                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun InfoTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DataUsageCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Лимит трафика", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("1.2 ГБ / 5.0 ГБ (За сегодня)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 1.2f / 5.0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        }
    }
}


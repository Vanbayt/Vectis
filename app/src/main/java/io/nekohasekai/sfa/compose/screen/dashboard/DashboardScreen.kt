package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    viewModel: DashboardViewModel? = null,
) {
    val isConnected = serviceStatus == Status.Started || serviceStatus == Status.Starting

    OverrideTopBar {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Vectis",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: Navigate to Profile */ }) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { /* TODO: Navigate to Settings */ }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
    ) {
        
        // Cards Grid area (scrollable if needed, expands to push slider down)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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

            // Row 1: Traffic Stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoTile(
                    modifier = Modifier.weight(1f),
                    title = "Download",
                    value = "0 B/s", // TODO
                    icon = Icons.Rounded.ArrowDownward
                )
                InfoTile(
                    modifier = Modifier.weight(1f),
                    title = "Upload",
                    value = "0 B/s", // TODO
                    icon = Icons.Rounded.ArrowUpward
                )
            }

            // Row 2: Location and Protocol
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Location Card
                InfoTile(
                    modifier = Modifier.weight(1f),
                    title = "Location",
                    value = "Frankfurt", // TODO
                    icon = Icons.Rounded.Place
                )
                // Protocol Card
                InfoTile(
                    modifier = Modifier.weight(1f),
                    title = "Protocol",
                    value = "VLESS", // TODO
                    icon = Icons.Rounded.Lock
                )
            }
            
            // Row 3: Account/Subscription Card
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    ),
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
                        text = "14 days left", // TODO
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                onConnect = {
                    if (serviceStatus == Status.Stopped) viewModel?.toggleService()
                },
                onDisconnect = {
                    if (serviceStatus == Status.Started || serviceStatus == Status.Starting) viewModel?.toggleService()
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
            Text("Uptime: 00:00", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Text("  •  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ping: 45 ms", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwipeToConnectSlider(
    isConnected: Boolean,
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

    // Sync external state changes with the slider thumb
    LaunchedEffect(isConnected) {
        if (isConnected) {
            offsetX.animateTo(maxSwipePx)
        } else {
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
            .background(backgroundColor),
        contentAlignment = Alignment.CenterStart
    ) {
        // Centered Text
        val textAlpha = 1f - (offsetX.value / maxSwipePx * 1.5f).coerceIn(0f, 1f)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isConnected) "Swipe to disconnect" else "Swipe to connect",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(textAlpha * 0.5f)
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(start = padding)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            lastHapticPosition = 0f
                            coroutineScope.launch {
                                if (offsetX.value > maxSwipePx * 0.7f) {
                                    offsetX.animateTo(maxSwipePx, spring())
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onConnect()
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 200f))
                                    onDisconnect()
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
                                val resistance = 1f - (offsetX.value / maxSwipePx * 0.6f)
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
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary
            ),
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

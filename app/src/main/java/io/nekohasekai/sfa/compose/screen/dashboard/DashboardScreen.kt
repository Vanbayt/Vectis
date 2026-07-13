package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable

@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToTraffic: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val isConnected = serviceStatus == Status.Started || serviceStatus == Status.Starting
    val isConnecting = serviceStatus == Status.Starting
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()

    // Подписываемся на ошибки из ViewModel
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

    // Staggered Entrance Animation State
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
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
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", modifier = Modifier.size(28.dp))
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
                Spacer(modifier = Modifier.height(4.dp))

                // Hero Element: Data Usage
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = LinearOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 0))
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp).bounceClick { }) {
                        DataUsageCard(
                            trafficUsed = state.trafficUsed,
                            trafficLimit = state.trafficLimit,
                            onClick = onNavigateToTraffic
                        )
                    }
                }

                // 2x2 Grid for Stats & Info
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(durationMillis = 400, delayMillis = 100, easing = LinearOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 100))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InfoTile(
                                modifier = Modifier.weight(1f).bounceClick { },
                                title = "Download",
                                value = state.downlink,
                                icon = Icons.Rounded.ArrowDownward,
                                shape = RoundedCornerShape(topStart = 32.dp, bottomEnd = 4.dp, topEnd = 4.dp, bottomStart = 4.dp)
                            )
                            InfoTile(
                                modifier = Modifier.weight(1f).bounceClick { },
                                title = "Upload",
                                value = state.uplink,
                                icon = Icons.Rounded.ArrowUpward,
                                shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp, topEnd = 32.dp, bottomStart = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InfoTile(
                                modifier = Modifier.weight(1f).bounceClick { },
                                title = "Location",
                                value = state.location,
                                icon = Icons.Rounded.Place,
                                shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 4.dp, topEnd = 4.dp, bottomStart = 32.dp)
                            )
                            InfoTile(
                                modifier = Modifier.weight(1f).bounceClick { },
                                title = "Protocol",
                                value = state.protocol,
                                icon = Icons.Rounded.Lock,
                                shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 32.dp, topEnd = 4.dp, bottomStart = 4.dp)
                            )
                        }
                    }
                }

                // Account/Subscription Card
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(durationMillis = 400, delayMillis = 200, easing = LinearOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400, delayMillis = 200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .bounceClick { },
                        shape = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 8.dp, bottomStart = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Premium Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "14 days left",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = "Subscription",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
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
                AnimatedContent(
                    targetState = state.ping,
                    transitionSpec = {
                        slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() togetherWith
                                slideOutVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) { height -> -height } + fadeOut()
                    },
                    label = "ping"
                ) { targetPing ->
                    Text("Ping: $targetPing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
    
    val width = 340.dp
    val height = 88.dp
    val thumbSize = 72.dp
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "sliderBackground"
    )

    // M3 Expressive Shape Morphing
    val containerShapePercent by animateIntAsState(
        targetValue = if (isConnected) 50 else 25,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "containerShape"
    )
    val thumbShapePercent by animateIntAsState(
        targetValue = if (offsetX.value > 5f && offsetX.value < maxSwipePx - 5f) 25 else 50,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "thumbShape"
    )

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .bounceClick { if (isConnected && !isConnecting) onDisconnect() }
            .clip(RoundedCornerShape(percent = containerShapePercent))
            .background(backgroundColor),
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
                    modifier = Modifier.alpha(textAlpha * 0.7f)
                )
            }
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(start = padding)
                .size(thumbSize)
                .clip(RoundedCornerShape(percent = thumbShapePercent))
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
    icon: ImageVector,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) {
    Card(
        modifier = modifier.height(90.dp).animateContentSize(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Increased padding for M3 standard look
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() togetherWith
                                slideOutVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) { height -> -height } + fadeOut()
                    },
                    label = "valueAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DataUsageCard(trafficUsed: Long = 0L, trafficLimit: Long = 5L * 1024 * 1024 * 1024, onClick: () -> Unit = {}) {
    val usedGb = trafficUsed.toFloat() / (1024 * 1024 * 1024)
    val limitGb = trafficLimit.toFloat() / (1024 * 1024 * 1024)
    val remainingGb = maxOf(0f, limitGb - usedGb)
    val progress = if (limitGb > 0) (usedGb / limitGb).coerceIn(0f, 1f) else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp).clickable { onClick() }.animateContentSize(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Лимит трафика", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("За сегодня", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Осталось", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(String.format("%.1f ГБ", remainingGb), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Text(String.format("%.1f / %.1f", usedGb, limitGb), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}


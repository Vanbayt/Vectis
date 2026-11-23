package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
    onNavigateToProtocolSelection: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
) {
    val isConnected = serviceStatus == Status.Started || serviceStatus == Status.Starting
    val isConnecting = serviceStatus == Status.Starting
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val notifState by notificationsViewModel.uiState.collectAsState()
    val hasUnreadNotifications = remember(notifState.notifications) {
        notifState.notifications.any { !it.is_read }
    }

    var showUpgradeDialog by remember { mutableStateOf(false) }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Доступно в тарифе Maximum") },
            text = {
                Text("Ручной выбор конкретного протокола и сервера доступен для пользователей с подпиской Maximum. Обновите тариф в профиле!")
            },
            confirmButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    // Подписываемся на ошибки из ViewModel
    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    // Staggered Entrance Animation State
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.refreshLocationAndProtocol()
        delay(100)
        visible = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                        BadgedBox(
                            badge = {
                                if (hasUnreadNotifications) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", modifier = Modifier.size(28.dp))
                        }
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
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.dashboard_download),
                                value = state.downlink,
                                icon = Icons.Rounded.ArrowDownward,
                                shape = RoundedCornerShape(20.dp)
                            )
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.dashboard_upload),
                                value = state.uplink,
                                icon = Icons.Rounded.ArrowUpward,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoTile(
                                modifier = Modifier.weight(1f),
                                title = stringResource(R.string.dashboard_location),
                                value = state.location,
                                icon = Icons.Rounded.Place,
                                shape = RoundedCornerShape(20.dp)
                            )
                            InfoTile(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (io.nekohasekai.sfa.utils.SubscriptionAccess.canSelectProtocol(state.userProfile?.subscription_tier)) {
                                            onNavigateToProtocolSelection()
                                        } else {
                                            showUpgradeDialog = true
                                        }
                                    },
                                title = stringResource(R.string.dashboard_protocol),
                                value = state.protocol,
                                icon = Icons.Rounded.Lock,
                                shape = RoundedCornerShape(20.dp)
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
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onNavigateToProfile),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                val unlimitedTimeStr = stringResource(R.string.profile_sub_unlimited_time)
                                val expiredStr = stringResource(R.string.profile_sub_expired)
                                Text(
                                    text = stringResource(R.string.profile_sub_info_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                val daysLeftText = remember(state.userProfile?.subscription_end) {
                                    val endStr = state.userProfile?.subscription_end
                                    if (endStr == null) {
                                        unlimitedTimeStr
                                    } else {
                                        try {
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                                            }
                                            val date = sdf.parse(endStr)
                                            if (date != null) {
                                                val diff = date.time - System.currentTimeMillis()
                                                val days = diff / (1000 * 60 * 60 * 24)
                                                val outSdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                                if (days < 0) {
                                                    expiredStr
                                                } else {
                                                    context.getString(R.string.profile_sub_active_until, outSdf.format(date)) + " (${days.toInt()} дн.)"
                                                }
                                            } else {
                                                endStr
                                            }
                                        } catch (_: Exception) {
                                            endStr
                                        }
                                    }
                                }
                                Text(
                                    text = daysLeftText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
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
                DashboardUptimeText(
                    startTime = state.serviceStartTime,
                    isConnected = isConnected
                )
                
                Text("  •  ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Crossfade(
                    targetState = state.ping,
                    animationSpec = tween(durationMillis = 200),
                    label = "pingAnimation"
                ) { targetPing ->
                    Text(
                        text = stringResource(R.string.dashboard_ping, targetPing),
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
fun DashboardUptimeText(
    startTime: Long?,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (!isConnected || startTime == null) {
        Text(
            text = stringResource(R.string.dashboard_uptime, "00:00"),
            style = style.copy(fontFeatureSettings = "tnum"),
            color = color,
            modifier = modifier
        )
        return
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTime) {
        while (isActive) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val startMs = if (startTime > 1000000000000L) startTime else startTime * 1000
    val elapsedSeconds = ((currentTime - startMs) / 1000).coerceAtLeast(0)
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    Text(
        text = stringResource(R.string.dashboard_uptime, formattedTime),
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        modifier = modifier
    )
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val thumbSizeDp = 64.dp
        val paddingDp = 8.dp
        val thumbSizePx = with(density) { thumbSizeDp.toPx() }
        val paddingPx = with(density) { paddingDp.toPx() }
        val maxSwipePx = (totalWidthPx - thumbSizePx - (paddingPx * 2)).coerceAtLeast(0f)

        LaunchedEffect(isConnected, isConnecting, maxSwipePx) {
            if (isConnected) {
                offsetX.animateTo(maxSwipePx, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            } else if (!isConnecting) {
                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
        }

        val backgroundColor by animateColorAsState(
            targetValue = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
            label = "sliderBackground"
        )

        // M3 Expressive morphing: 24dp (rounded container) -> 40dp (pill)
        val containerRadiusDp by animateDpAsState(
            targetValue = if (isConnected) 40.dp else 24.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "containerShape"
        )
        val isDragging = offsetX.value > 5f && offsetX.value < maxSwipePx - 5f
        val thumbRadiusDp by animateDpAsState(
            targetValue = if (isDragging) 20.dp else 32.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "thumbShape"
        )

        val dragChannel = remember { kotlinx.coroutines.channels.Channel<Float>(kotlinx.coroutines.channels.Channel.CONFLATED) }
        LaunchedEffect(dragChannel, isConnected, maxSwipePx) {
            for (dragAmount in dragChannel) {
                if (maxSwipePx <= 0f) continue
                val resistance = if (isConnected) {
                    (offsetX.value / maxSwipePx * 0.5f) + 0.5f
                } else {
                    1f - (offsetX.value / maxSwipePx * 0.5f)
                }
                offsetX.snapTo((offsetX.value + dragAmount * resistance).coerceIn(0f, maxSwipePx))

                if (abs(offsetX.value - lastHapticPosition) > 40f) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticPosition = offsetX.value
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(containerRadiusDp))
                .background(backgroundColor),
            contentAlignment = Alignment.CenterStart
        ) {
            // Helper text that smoothly fades out as thumb moves
            val textProgress = if (maxSwipePx > 0f) (offsetX.value / maxSwipePx) else 0f
            val textAlpha = (1f - textProgress * 1.8f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = paddingDp + thumbSizeDp),
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (isConnected) stringResource(R.string.dashboard_swipe_to_disconnect) else stringResource(R.string.dashboard_swipe_to_connect),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alpha(textAlpha * 0.8f)
                    )
                }
            }

            // Draggable thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .padding(start = paddingDp)
                    .size(thumbSizeDp)
                    .clip(RoundedCornerShape(thumbRadiusDp))
                    .background(if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface)
                    .pointerInput(isConnecting, isConnected, maxSwipePx) {
                        if (isConnecting || maxSwipePx <= 0f) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                lastHapticPosition = 0f
                                coroutineScope.launch {
                                    if (isConnected) {
                                        if (offsetX.value < maxSwipePx * 0.7f) {
                                            offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 250f))
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDisconnect()
                                        } else {
                                            offsetX.animateTo(maxSwipePx, spring())
                                        }
                                    } else {
                                        if (offsetX.value > maxSwipePx * 0.7f) {
                                            offsetX.animateTo(maxSwipePx, spring())
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onConnect()
                                        } else {
                                            offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 250f))
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                lastHapticPosition = 0f
                                coroutineScope.launch {
                                    offsetX.animateTo(if (isConnected) maxSwipePx else 0f, spring(dampingRatio = 0.6f, stiffness = 250f))
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragChannel.trySend(dragAmount)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isConnected,
                    animationSpec = tween(200),
                    label = "thumbIcon"
                ) { connected ->
                    Icon(
                        imageVector = if (connected) Icons.Rounded.PowerSettingsNew else Icons.Rounded.Lock,
                        contentDescription = "Swipe thumb",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
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
        modifier = modifier.height(92.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Crossfade(
                    targetState = value,
                    animationSpec = tween(durationMillis = 200),
                    label = "valueAnimation"
                ) { targetValue ->
                    Text(
                        text = targetValue,
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DataUsageCard(
    trafficUsed: Long = 0L,
    trafficLimit: Long = 5L * 1024 * 1024 * 1024,
    onClick: () -> Unit = {}
) {
    val usedGb = trafficUsed.toFloat() / (1024 * 1024 * 1024)
    val limitGb = trafficLimit.toFloat() / (1024 * 1024 * 1024)
    val targetProgress = if (limitGb > 0) (usedGb / limitGb).coerceIn(0f, 1f) else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "dataUsageProgress"
    )

    val context = LocalContext.current
    val formatBytes: (Long) -> String = remember(context) {
        { bytes ->
            when {
                bytes >= 1024 * 1024 * 1024 -> context.getString(R.string.unit_gb, String.format(Locale.getDefault(), "%.2f", bytes.toFloat() / (1024 * 1024 * 1024)))
                bytes >= 1024 * 1024 -> context.getString(R.string.unit_mb, String.format(Locale.getDefault(), "%.2f", bytes.toFloat() / (1024 * 1024)))
                bytes >= 1024 -> context.getString(R.string.unit_kb, String.format(Locale.getDefault(), "%.2f", bytes.toFloat() / 1024))
                else -> context.getString(R.string.unit_b, bytes.toString())
            }
        }
    }

    val usedStr = remember(trafficUsed) { formatBytes(trafficUsed) }
    val limitStr = remember(trafficLimit) { if (trafficLimit <= 0L) "∞" else formatBytes(trafficLimit) }
    val remainingBytes = maxOf(0L, trafficLimit - trafficUsed)
    val remainingStr = remember(remainingBytes, trafficLimit) { if (trafficLimit <= 0L) "∞" else formatBytes(remainingBytes) }

    val isNearLimit = trafficLimit > 0L && targetProgress >= 0.9f
    val progressColor by animateColorAsState(
        targetValue = if (isNearLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "progressColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.traffic_limit_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.dashboard_all_time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.traffic_remaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = remainingStr,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$usedStr / $limitStr",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    if (isNearLimit) {
                        Text(
                            text = "< 10%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


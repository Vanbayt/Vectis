package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.compose.screen.login.WavyCookieShape
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import io.nekohasekai.sfa.constant.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    viewModel: DashboardViewModel? = null,
) {
    val isRunning = serviceStatus == Status.Started || serviceStatus == Status.Starting
    val isStarting = serviceStatus == Status.Starting
    val isConnected = serviceStatus == Status.Started

    // Pulse Animations (Radar Effect)
    val infinitePulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infinitePulse.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale1"
    )
    val pulseAlpha1 by infinitePulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha1"
    )

    val pulseScale2 by infinitePulse.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale2"
    )
    val pulseAlpha2 by infinitePulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha2"
    )

    OverrideTopBar {
        TopAppBar(
            title = {
                Text(
                    text = "Vectis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
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
            .padding(horizontal = 24.dp)
            .padding(bottom = 64.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        
        // Top Section: Info Cards (Time, Location, Protocol)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Card: Subscription
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = MaterialTheme.colorScheme.primary,
                        spotColor = MaterialTheme.colorScheme.primary
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DateRange,
                        contentDescription = "Subscription",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Подписка",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "TODO: 14 дней",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Grid: Location and Protocol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Location Card
                InfoTile(
                    modifier = Modifier.weight(1f),
                    title = "Локация",
                    value = "Frankfurt, DE", // TODO
                    icon = Icons.Rounded.Place
                )

                // Protocol Card
                InfoTile(
                    modifier = Modifier.weight(1f),
                    title = "Протокол",
                    value = "VLESS Reality", // TODO
                    icon = Icons.Rounded.Lock
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Statistics Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Download Card
            InfoTile(
                modifier = Modifier.weight(1f),
                title = "Download",
                value = "0 KB/s", // TODO
                icon = Icons.Rounded.ArrowDownward
            )

            // Upload Card
            InfoTile(
                modifier = Modifier.weight(1f),
                title = "Upload",
                value = "0 KB/s", // TODO
                icon = Icons.Rounded.ArrowUpward
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status Text & Indicator
        val statusText = when {
            serviceStatus == Status.Started -> "Подключено"
            serviceStatus == Status.Starting -> "Подключение..."
            serviceStatus == Status.Stopping -> "Отключение..."
            else -> "Отключено"
        }
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Connection Button with Radar Effect
        val interactionSource = remember { MutableInteractionSource() }
        val buttonSize = 160.dp
        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // Radar Effect (Only when connecting/connected)
            if (isRunning) {
                // Circle 1
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .scale(pulseScale1)
                        .graphicsLayer { alpha = pulseAlpha1 }
                        .clip(WavyCookieShape(points = 12, waveDepth = 0.05f))
                        .background(MaterialTheme.colorScheme.primary)
                )
                // Circle 2
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .scale(pulseScale2)
                        .graphicsLayer { alpha = pulseAlpha2 }
                        .clip(WavyCookieShape(points = 12, waveDepth = 0.05f))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            // Main Circular Button
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(WavyCookieShape(points = 12, waveDepth = 0.05f))
                    .background(
                        if (isConnected) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null, // Disable default ripple to keep the shape clean
                        onClick = { viewModel?.toggleService() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = "Start/Stop VPN",
                    tint = if (isConnected) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
            }
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
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

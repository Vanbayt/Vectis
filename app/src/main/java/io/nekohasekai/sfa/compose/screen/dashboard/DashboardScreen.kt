package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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

    // Animation states for the button
    val infiniteTransition = rememberInfiniteTransition(label = "buttonRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isStarting) 2000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Breathing Halo Animation
    val breathingTransition = rememberInfiniteTransition(label = "breathingHalo")
    val haloScale by breathingTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloScale"
    )
    val haloAlpha by breathingTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloAlpha"
    )

    Scaffold(
        topBar = {
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
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .padding(bottom = 64.dp, top = 16.dp),
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

            // Status Text
            val statusText = when {
                serviceStatus == Status.Started -> "Подключено"
                serviceStatus == Status.Starting -> "Подключение..."
                serviceStatus == Status.Stopping -> "Отключение..."
                else -> "Отключено"
            }

            AnimatedContent(
                targetState = statusText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "StatusTextAnimation"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Connection Button with Breathing Halo
            val interactionSource = remember { MutableInteractionSource() }
            
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Breathing Halo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale * haloScale) // Base scale + breathing expansion
                        .graphicsLayer {
                            rotationZ = if (isRunning) rotation else 0f
                            alpha = haloAlpha
                        }
                        .clip(WavyCookieShape(points = 12, waveDepth = 0.15f))
                        .background(MaterialTheme.colorScheme.primary)
                )

                // Main Button
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .graphicsLayer {
                            // Apply rotation only if it's running or starting, else animate to 0 or stay static
                            rotationZ = if (isRunning) rotation else 0f
                        }
                        .clip(WavyCookieShape(points = 12, waveDepth = 0.15f))
                        .background(
                            if (isConnected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null, // Disable default ripple to keep the shape clean
                            onClick = { viewModel?.toggleService() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRunning) "Stop" else "Start",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isConnected) MaterialTheme.colorScheme.onPrimary 
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                        // Counter-rotate text so it stays upright
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (isRunning) -rotation else 0f
                        }
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

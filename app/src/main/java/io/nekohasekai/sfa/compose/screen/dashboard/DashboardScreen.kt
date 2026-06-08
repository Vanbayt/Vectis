package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.screen.login.WavyCookieShape
import io.nekohasekai.sfa.constant.Status
import kotlinx.coroutines.delay

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
                .padding(bottom = 64.dp, top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Top Section: Info Cards (Time, Location, Protocol)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mock Time Left
                InfoCard(
                    title = "Подписка",
                    value = "TODO: 14 дней"
                )
                
                // Mock Location
                InfoCard(
                    title = "Локация",
                    value = "TODO: Frankfurt, DE"
                )

                // Mock Protocol
                InfoCard(
                    title = "Протокол",
                    value = "TODO: VLESS Reality"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status Text
            val statusText = when {
                serviceStatus == Status.Started -> "Подключено"
                serviceStatus == Status.Starting -> "Подключение..."
                serviceStatus == Status.Stopping -> "Отключение..."
                else -> "Отключено"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Connection Button
            val interactionSource = remember { MutableInteractionSource() }
            
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .graphicsLayer {
                        // Apply rotation only if it's running or starting, else animate to 0 or stay static
                        // For a smoother stop, we could animate rotation back to 0, but graphicsLayer rotation is fine
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

@Composable
fun InfoCard(title: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package io.nekohasekai.sfa.compose.screen.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.utils.HookModuleUpdateNotifier
import io.nekohasekai.sfa.utils.HookStatusClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val hookStatus by HookStatusClient.status.collectAsState()
    val hasPendingPrivilegeDowngrade = HookModuleUpdateNotifier.isDowngrade(hookStatus)
    val hasPendingPrivilegeUpdate = HookModuleUpdateNotifier.isUpgrade(hookStatus)
    LaunchedEffect(Unit) {
        HookStatusClient.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.title_settings), 
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Main & Per-App Proxy
            Text(
                text = stringResource(R.string.settings_section_main),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Apps,
                        title = stringResource(R.string.per_app_proxy),
                        subtitle = stringResource(R.string.per_app_proxy_subtitle),
                        onClick = { navController.navigate("settings/profile_override/manage") }
                    )
                    SettingTile(
                        icon = Icons.Outlined.FilterAlt,
                        title = stringResource(R.string.profile_override),
                        subtitle = stringResource(R.string.profile_override_subtitle),
                        onClick = { navController.navigate("settings/profile_override") }
                    )
                }
            }

            // Section 2: General & Appearance
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.title_app_settings),
                        subtitle = stringResource(R.string.app_settings_subtitle),
                        onClick = { navController.navigate("settings/app") }
                    )
                }
            }

            // Section 3: Core & Service
            Text(
                text = stringResource(R.string.settings_section_core),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.service),
                        subtitle = stringResource(R.string.service_settings_subtitle),
                        onClick = { navController.navigate("settings/service") }
                    )
                    SettingTile(
                        icon = Icons.Outlined.Settings,
                        title = stringResource(R.string.core),
                        subtitle = stringResource(R.string.core_settings_subtitle),
                        onClick = { navController.navigate("settings/core") }
                    )
                    SettingTile(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = stringResource(R.string.privilege_settings),
                        subtitle = stringResource(R.string.privilege_settings_subtitle),
                        onClick = { navController.navigate("settings/privilege") },
                        hasBadge = hasPendingPrivilegeDowngrade || hasPendingPrivilegeUpdate,
                        badgeColor = if (hasPendingPrivilegeDowngrade) MaterialTheme.colorScheme.error else Color(0xFFFFC107)
                    )
                }
            }

            // Section 4: Diagnostics & Tools
            Text(
                text = stringResource(R.string.settings_section_tools),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Terminal,
                        title = stringResource(R.string.title_tools),
                        subtitle = stringResource(R.string.tools_settings_subtitle),
                        onClick = { navController.navigate("tools") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    hasBadge: Boolean = false,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasBadge || badgeText != null) {
                Badge(containerColor = badgeColor) {
                    if (badgeText != null) {
                        Text(badgeText)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

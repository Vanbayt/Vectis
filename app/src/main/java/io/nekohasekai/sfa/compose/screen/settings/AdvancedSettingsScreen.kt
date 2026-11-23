package io.nekohasekai.sfa.compose.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.utils.HookModuleUpdateNotifier
import io.nekohasekai.sfa.utils.HookStatusClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(navController: NavController) {
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
                        text = stringResource(R.string.advanced_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section 1: Service & Network Mode
            Text(
                text = stringResource(R.string.settings_section_core),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.service),
                        subtitle = if (Settings.serviceMode == io.nekohasekai.sfa.constant.ServiceMode.ROOT_TUN) {
                            stringResource(R.string.service_mode_root)
                        } else {
                            stringResource(R.string.service_mode_standard)
                        },
                        onClick = { navController.navigate("settings/service") },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    SettingTile(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = stringResource(R.string.privilege_settings),
                        subtitle = stringResource(R.string.privilege_settings_subtitle),
                        onClick = { navController.navigate("settings/privilege") },
                        hasBadge = hasPendingPrivilegeDowngrade || hasPendingPrivilegeUpdate,
                        badgeColor = if (hasPendingPrivilegeDowngrade) MaterialTheme.colorScheme.error else Color(0xFFFFC107),
                    )
                }
            }

            // Section 2: Core & Diagnostics
            Text(
                text = stringResource(R.string.settings_section_tools),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Settings,
                        title = stringResource(R.string.core),
                        subtitle = stringResource(R.string.core_settings_subtitle),
                        onClick = { navController.navigate("settings/core") },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    SettingTile(
                        icon = Icons.Outlined.Terminal,
                        title = stringResource(R.string.title_tools),
                        subtitle = stringResource(R.string.tools_settings_subtitle),
                        onClick = { navController.navigate("tools") },
                    )
                }
            }

            // Section 3: Tailscale Terminal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SettingTile(
                        icon = Icons.Outlined.Terminal,
                        title = "Tailscale SSH Terminal",
                        subtitle = "Темы, шрифты и настройки терминала",
                        onClick = { navController.navigate("settings/tailscale/terminal_config") },
                    )
                }
            }
        }
    }
}

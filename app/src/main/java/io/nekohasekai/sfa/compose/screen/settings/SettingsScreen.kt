package io.nekohasekai.sfa.compose.screen.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.base.GlobalEventBus
import io.nekohasekai.sfa.compose.base.UiEvent
import io.nekohasekai.sfa.compose.base.rememberApplyServiceChangeNotifier
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.update.UpdateState
import io.nekohasekai.sfa.utils.HookModuleUpdateNotifier
import io.nekohasekai.sfa.utils.HookStatusClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    serviceStatus: Status = Status.Stopped,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hookStatus by HookStatusClient.status.collectAsState()
    val hasPendingPrivilegeDowngrade = HookModuleUpdateNotifier.isDowngrade(hookStatus)
    val hasPendingPrivilegeUpdate = HookModuleUpdateNotifier.isUpgrade(hookStatus)
    val hasAppUpdate by UpdateState.hasUpdate
    val notifyApplyChange = rememberApplyServiceChangeNotifier(serviceStatus)

    var currentRoutingMode by remember { mutableStateOf(Settings.routingMode) }
    var currentAdBlock by remember { mutableStateOf(Settings.adblockEnabled) }
    var currentAutoBoot by remember { mutableStateOf(Settings.autoConnectOnBoot) }
    var showKillSwitchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        HookStatusClient.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
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
            // Section 1: Account & Profile
            Text(
                text = stringResource(R.string.settings_section_account),
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
                        icon = Icons.Outlined.Person,
                        title = stringResource(R.string.my_account_title),
                        subtitle = "Управление подпиской, смена пароля и безопасность",
                        onClick = { navController.navigate("profile") },
                    )
                }
            }

            // Section 2: Routing Mode (Main choice for regular users)
            Text(
                text = stringResource(R.string.settings_section_routing),
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
                    RoutingModeItem(
                        title = stringResource(R.string.routing_mode_smart),
                        description = stringResource(R.string.routing_mode_smart_desc),
                        selected = currentRoutingMode == Settings.ROUTING_MODE_SMART,
                        onSelect = {
                            if (currentRoutingMode != Settings.ROUTING_MODE_SMART) {
                                currentRoutingMode = Settings.ROUTING_MODE_SMART
                                coroutineScope.launch(Dispatchers.IO) {
                                    Settings.routingMode = Settings.ROUTING_MODE_SMART
                                    withContext(Dispatchers.Main) {
                                        if (serviceStatus == Status.Started) {
                                            GlobalEventBus.tryEmit(UiEvent.RequestRestartService)
                                        }
                                    }
                                }
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    RoutingModeItem(
                        title = stringResource(R.string.routing_mode_all),
                        description = stringResource(R.string.routing_mode_all_desc),
                        selected = currentRoutingMode == Settings.ROUTING_MODE_ALL,
                        onSelect = {
                            if (currentRoutingMode != Settings.ROUTING_MODE_ALL) {
                                currentRoutingMode = Settings.ROUTING_MODE_ALL
                                coroutineScope.launch(Dispatchers.IO) {
                                    Settings.routingMode = Settings.ROUTING_MODE_ALL
                                    withContext(Dispatchers.Main) {
                                        if (serviceStatus == Status.Started) {
                                            GlobalEventBus.tryEmit(UiEvent.RequestRestartService)
                                        }
                                    }
                                }
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    RoutingModeItem(
                        title = stringResource(R.string.routing_mode_blocked),
                        description = stringResource(R.string.routing_mode_blocked_desc),
                        selected = currentRoutingMode == Settings.ROUTING_MODE_BLOCKED,
                        onSelect = {
                            if (currentRoutingMode != Settings.ROUTING_MODE_BLOCKED) {
                                currentRoutingMode = Settings.ROUTING_MODE_BLOCKED
                                coroutineScope.launch(Dispatchers.IO) {
                                    Settings.routingMode = Settings.ROUTING_MODE_BLOCKED
                                    withContext(Dispatchers.Main) {
                                        if (serviceStatus == Status.Started) {
                                            GlobalEventBus.tryEmit(UiEvent.RequestRestartService)
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }

            // Section 3: Security & Rules (Per-App, AdBlock, Auto-connect, Kill Switch)
            Text(
                text = stringResource(R.string.settings_section_security_automation),
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
                        icon = Icons.Outlined.Apps,
                        title = stringResource(R.string.per_app_proxy),
                        subtitle = if (Settings.perAppProxyEnabled) {
                            stringResource(R.string.per_app_proxy_enabled_summary, Settings.getEffectivePerAppProxyList().size)
                        } else {
                            stringResource(R.string.per_app_proxy_subtitle)
                        },
                        onClick = { navController.navigate("settings/profile_override/manage") },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    SettingSwitchTile(
                        icon = Icons.Filled.Shield,
                        title = stringResource(R.string.adblock_title),
                        subtitle = stringResource(R.string.adblock_desc),
                        checked = currentAdBlock,
                        onCheckedChange = { isChecked ->
                            currentAdBlock = isChecked
                            coroutineScope.launch(Dispatchers.IO) {
                                Settings.adblockEnabled = isChecked
                                withContext(Dispatchers.Main) {
                                    if (serviceStatus == Status.Started) {
                                        GlobalEventBus.tryEmit(UiEvent.RequestRestartService)
                                    }
                                }
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    SettingSwitchTile(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.auto_connect_on_boot),
                        subtitle = stringResource(R.string.auto_connect_on_boot_desc),
                        checked = currentAutoBoot,
                        onCheckedChange = { isChecked ->
                            currentAutoBoot = isChecked
                            coroutineScope.launch(Dispatchers.IO) {
                                Settings.autoConnectOnBoot = isChecked
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    SettingTile(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.kill_switch_title),
                        subtitle = stringResource(R.string.kill_switch_desc),
                        onClick = { showKillSwitchDialog = true },
                    )
                }
            }

            // Section 4: App & Appearance
            Text(
                text = stringResource(R.string.settings_section_appearance),
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
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.title_app_settings),
                        subtitle = stringResource(R.string.app_settings_subtitle),
                        onClick = { navController.navigate("settings/app") },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    SettingTile(
                        icon = Icons.Outlined.AppShortcut,
                        title = stringResource(R.string.quick_settings_tile_title),
                        subtitle = stringResource(R.string.quick_settings_tile_desc),
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val statusBarManager = context.getSystemService(android.app.StatusBarManager::class.java)
                                statusBarManager?.requestAddTileService(
                                    android.content.ComponentName(context, io.nekohasekai.sfa.bg.TileService::class.java),
                                    context.getString(R.string.app_name),
                                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_menu),
                                    context.mainExecutor,
                                ) { result ->
                                    if (result == android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                        Toast.makeText(context, context.getString(R.string.quick_settings_tile_added), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.quick_settings_tile_manual), Toast.LENGTH_LONG).show()
                            }
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )

                    val currentVersion = BuildConfig.VERSION_NAME
                    SettingTile(
                        icon = Icons.Outlined.SystemUpdate,
                        title = stringResource(R.string.check_for_updates),
                        subtitle = stringResource(R.string.check_for_updates_subtitle, currentVersion),
                        hasBadge = hasAppUpdate,
                        badgeText = if (hasAppUpdate) "NEW" else null,
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    Toast.makeText(context, context.getString(R.string.settings_checking_updates), Toast.LENGTH_SHORT).show()
                                    val updateInfo = withContext(Dispatchers.IO) {
                                        io.nekohasekai.sfa.vendor.Vendor.checkUpdateAsync()
                                    }
                                    if (updateInfo != null) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_update_available, updateInfo.versionName),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        val downloadUrl = updateInfo.downloadUrl.ifBlank { updateInfo.releaseUrl }
                                        if (downloadUrl.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_latest_version, currentVersion),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Ошибка проверки: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
            }

            // Section 5: Advanced Settings (All low-level technical options secluded here)
            Text(
                text = stringResource(R.string.settings_section_advanced),
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
                        title = stringResource(R.string.advanced_settings_title),
                        subtitle = stringResource(R.string.advanced_settings_subtitle),
                        onClick = { navController.navigate("settings/advanced") },
                        hasBadge = hasPendingPrivilegeDowngrade || hasPendingPrivilegeUpdate,
                        badgeColor = if (hasPendingPrivilegeDowngrade) MaterialTheme.colorScheme.error else Color(0xFFFFC107),
                    )
                }
            }
        }
    }

    if (showKillSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showKillSwitchDialog = false },
            title = { Text(stringResource(R.string.kill_switch_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.kill_switch_dialog_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showKillSwitchDialog = false
                        try {
                            val intent = Intent(AndroidSettings.ACTION_VPN_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Не удалось открыть настройки VPN", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.kill_switch_dialog_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillSwitchDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun RoutingModeItem(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun SettingSwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun SettingTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    hasBadge: Boolean = false,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.error,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

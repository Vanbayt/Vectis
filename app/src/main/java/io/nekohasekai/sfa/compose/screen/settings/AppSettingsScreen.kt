package io.nekohasekai.sfa.compose.screen.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.base.UiEvent
import io.nekohasekai.sfa.compose.base.rememberApplyServiceChangeNotifier
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.ktx.clipboardText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Locale
import android.provider.Settings as AndroidSettings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppSettingsScreen(
    navController: NavController,
    serviceStatus: Status = Status.Stopped,
) {
    OverrideTopBar {
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.title_app_settings), 
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_description_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showVersionMenu by remember { mutableStateOf(false) }

    var notificationEnabled by remember { mutableStateOf(true) }
    var dynamicNotification by remember { mutableStateOf(Settings.dynamicNotification) }
    var showDisableNotificationDialog by remember { mutableStateOf(false) }
    val notifyApplyChange = rememberApplyServiceChangeNotifier(serviceStatus)

    var showLanguageDialog by remember { mutableStateOf(false) }
    val availableLocales = remember { getSupportedLocales(context) }
    var currentLocaleTag by remember {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        mutableStateOf(if (appLocales.isEmpty) "" else appLocales.toLanguageTags())
    }

    var cacheSize by remember { mutableStateOf(0L) }
    var cacheSizeText by remember { mutableStateOf("") }

    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            val size = calculateDirSize(context.cacheDir)
            withContext(Dispatchers.Main) {
                cacheSize = size
                cacheSizeText = Formatter.formatFileSize(context, size)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Application.notification.createNotificationChannel(
                NotificationChannel(
                    "service",
                    "Service Notifications",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            val channel = Application.notification.getNotificationChannel("service")
            notificationEnabled = channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    if (showDisableNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showDisableNotificationDialog = false },
            title = { Text(stringResource(R.string.enable_notification)) },
            text = {
                Text(
                    stringResource(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            R.string.disable_notification_description
                        } else {
                            R.string.disable_notification_description_legacy
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisableNotificationDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startActivity(
                            Intent(AndroidSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                putExtra(AndroidSettings.EXTRA_CHANNEL_ID, "service")
                            },
                        )
                    } else {
                        context.startActivity(
                            Intent(
                                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableNotificationDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentTag = currentLocaleTag,
            availableLocales = availableLocales,
            onLocaleSelected = { tag ->
                currentLocaleTag = tag
                val localeList = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box {
                    SettingsTileM3(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.app_version_title),
                        subtitle = BuildConfig.VERSION_NAME,
                        onClick = { },
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { showVersionMenu = true }
                        )
                    )
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        DropdownMenu(
                            expanded = showVersionMenu,
                            onDismissRequest = { showVersionMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.per_app_proxy_action_copy)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    clipboardText = BuildConfig.VERSION_NAME
                                    Toast.makeText(
                                        context,
                                        R.string.copied_to_clipboard,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    showVersionMenu = false
                                },
                            )
                        }
                    }
                }
                SettingsTileM3(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.language),
                    subtitle = if (currentLocaleTag.isEmpty()) {
                        stringResource(R.string.system_default)
                    } else {
                        val locale = Locale.forLanguageTag(currentLocaleTag)
                        locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
                    },
                    onClick = { showLanguageDialog = true }
                )
                SettingsTileM3(
                    icon = Icons.Outlined.DeleteSweep,
                    title = stringResource(R.string.cache_size),
                    subtitle = if (cacheSizeText.isNotEmpty()) cacheSizeText else null,
                    onClick = { }
                )
                if (cacheSize > 0L) {
                    SettingsTileM3(
                        icon = Icons.Outlined.DeleteForever,
                        title = stringResource(R.string.clear_cache),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                                withContext(Dispatchers.Main) {
                                    cacheSize = 0L
                                    cacheSizeText = Formatter.formatFileSize(context, 0L)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Tailscale Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.tailscale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsTileM3(
                        icon = Icons.Default.Terminal,
                        title = stringResource(R.string.tailscale_terminal_config),
                        onClick = { navController.navigate("settings/tailscale/terminal_config") }
                    )
                }
            }
        }

        // Notification Group
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.notification_settings),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsTileM3(
                        icon = Icons.Outlined.Notifications,
                        title = stringResource(R.string.enable_notification),
                        onClick = { showDisableNotificationDialog = true },
                        trailing = {
                            Switch(
                                checked = notificationEnabled,
                                onCheckedChange = null,
                            )
                        }
                    )
                    SettingsTileM3(
                        icon = Icons.Outlined.Speed,
                        title = stringResource(R.string.dynamic_notification),
                        onClick = {
                            dynamicNotification = !dynamicNotification
                            scope.launch(Dispatchers.IO) {
                                Settings.dynamicNotification = dynamicNotification
                                withContext(Dispatchers.Main) {
                                    notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
                                }
                            }
                        },
                        trailing = {
                            Switch(
                                checked = dynamicNotification,
                                onCheckedChange = { checked ->
                                    dynamicNotification = checked
                                    scope.launch(Dispatchers.IO) {
                                        Settings.dynamicNotification = checked
                                        withContext(Dispatchers.Main) {
                                            notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
                                        }
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsTileM3(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
private fun LanguageDialog(
    currentTag: String,
    availableLocales: List<Locale>,
    onLocaleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLocaleSelected("") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = currentTag.isEmpty(),
                        onClick = { onLocaleSelected("") },
                    )
                    Text(
                        text = stringResource(R.string.system_default),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                availableLocales.forEach { locale ->
                    val tag = locale.toLanguageTag()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLocaleSelected(tag) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentTag == tag,
                            onClick = { onLocaleSelected(tag) },
                        )
                        Text(
                            text = locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun calculateDirSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) calculateDirSize(file) else file.length()
    }
    return size
}

private fun getSupportedLocales(context: Context): List<Locale> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeConfig = android.app.LocaleConfig(context)
        val localeList = localeConfig.supportedLocales ?: return emptyList()
        return (0 until localeList.size()).map { localeList.get(it) }
    }
    return parseLocalesConfig(context)
}

private fun parseLocalesConfig(context: Context): List<Locale> {
    val locales = mutableListOf<Locale>()
    try {
        val resId = context.resources.getIdentifier(
            "_generated_res_locale_config",
            "xml",
            context.packageName,
        )
        if (resId == 0) return emptyList()
        val parser = context.resources.getXml(resId)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                val name = parser.getAttributeValue(
                    "http://schemas.android.com/apk/res/android",
                    "name",
                )
                if (name != null) {
                    locales.add(Locale.forLanguageTag(name))
                }
            }
        }
    } catch (_: Exception) {
    }
    return locales
}

package io.nekohasekai.sfa.compose.screen.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.database.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailscaleTerminalConfigScreen(navController: NavController) {
    var lightTheme by remember { mutableStateOf(Settings.tailscaleSSHLightTheme) }
    var darkTheme by remember { mutableStateOf(Settings.tailscaleSSHDarkTheme) }
    var fontFamily by remember { mutableStateOf(Settings.tailscaleSSHFontFamily) }
    var customFontPath by remember { mutableStateOf(Settings.tailscaleSSHCustomFontPath) }
    var fontSize by remember { mutableIntStateOf(Settings.tailscaleSSHFontSize) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tailscale_terminal_config), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.tailscale_terminal_color_theme),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.tailscale_terminal_light_theme), style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    lightTheme.ifBlank { "Default" },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("settings/tailscale/theme_picker/${Uri.encode("false")}")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.tailscale_terminal_dark_theme), style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    darkTheme.ifBlank { "Default" },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("settings/tailscale/theme_picker/${Uri.encode("true")}")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.tailscale_terminal_font_config),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.tailscale_terminal_font), style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            val fontDisplayName = when {
                                customFontPath.isNotBlank() -> java.io.File(customFontPath).nameWithoutExtension
                                fontFamily.isNotBlank() -> fontFamily
                                else -> stringResource(R.string.tailscale_terminal_font_default)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    fontDisplayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("settings/tailscale/font_picker")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.tailscale_terminal_font_size), style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        if (fontSize > 8) {
                                            fontSize--
                                            Settings.tailscaleSSHFontSize = fontSize
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = fontSize > 8,
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "-",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    "$fontSize",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFeatureSettings = "tnum",
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        if (fontSize < 32) {
                                            fontSize++
                                            Settings.tailscaleSSHFontSize = fontSize
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = fontSize < 32,
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "+",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

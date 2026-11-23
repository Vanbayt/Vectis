package io.nekohasekai.sfa.compose.screen.tools

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.bg.OOMReport
import io.nekohasekai.sfa.bg.OOMReportFile
import io.nekohasekai.sfa.bg.OOMReportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OOMReportDetailScreen(navController: NavController, reportId: String) {
    val reports by OOMReportManager.reports.collectAsState()
    val report = reports.find { it.id == reportId }
    var files by remember { mutableStateOf<List<OOMReportFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var shareMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(report) {
        if (report != null) {
            withContext(Dispatchers.IO) {
                OOMReportManager.markAsRead(report)
                files = OOMReportManager.availableFiles(report)
            }
        }
        isLoading = false
    }

    val title = if (report != null) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(report.date)
    } else {
        reportId
    }

    val hasConfig = report != null && OOMReportManager.hasConfigFile(report)

    fun shareReport(includeConfig: Boolean) {
        val currentReport = report ?: return
        scope.launch {
            val zipFile = OOMReportManager.createZipArchive(currentReport, includeConfig)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.cache", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                actions = {
                    if (!isLoading && files.isNotEmpty()) {
                        if (hasConfig) {
                            IconButton(onClick = { shareMenuExpanded = true }) {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = shareMenuExpanded,
                                onDismissRequest = { shareMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.report_share)) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        shareReport(includeConfig = false)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.report_share_with_config)) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        shareReport(includeConfig = true)
                                    },
                                )
                            }
                        } else {
                            IconButton(onClick = { shareReport(includeConfig = false) }) {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        }
                        IconButton(onClick = {
                            scope.launch {
                                if (report != null) {
                                    OOMReportManager.delete(report)
                                }
                                navController.navigateUp()
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.report_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.report_section_files),
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
                        files.forEachIndexed { index, file ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                            val icon = when (file.kind) {
                                OOMReportFile.Kind.METADATA -> Icons.Default.DataObject
                                OOMReportFile.Kind.PROFILE -> Icons.Default.Terminal
                                OOMReportFile.Kind.CONFIG -> Icons.Outlined.Settings
                            }
                            ListItem(
                                headlineContent = {
                                    Text(
                                        file.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (file.kind == OOMReportFile.Kind.METADATA) {
                                        navController.navigate("tools/oom_report/$reportId/metadata")
                                    } else {
                                        navController.navigate("tools/oom_report/$reportId/file/${file.kind.name}")
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OOMReportMetadataScreen(navController: NavController, reportId: String) {
    val reports by OOMReportManager.reports.collectAsState()
    val report = reports.find { it.id == reportId }
    var entries by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(report) {
        if (report != null) {
            withContext(Dispatchers.IO) {
                entries = loadOOMMetadataEntries(report)
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_metadata), fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.report_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column {
                            entries.forEachIndexed { index, (key, value) ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                                ListItem(
                                    headlineContent = {
                                        Text(key, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    },
                                    supportingContent = {
                                        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadOOMMetadataEntries(report: OOMReport): List<Pair<String, String>> {
    val metadataFile = OOMReportManager.availableFiles(report)
        .find { it.kind == OOMReportFile.Kind.METADATA } ?: return emptyList()
    val content = metadataFile.file.readText()
    val json = runCatching { JSONObject(content) }.getOrNull() ?: return emptyList()
    return json.keys().asSequence()
        .mapNotNull { key ->
            val value = json.optString(key, "")
            if (value.isNotBlank()) key to value else null
        }
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OOMReportFileContentScreen(navController: NavController, reportId: String, fileKind: String) {
    val reports by OOMReportManager.reports.collectAsState()
    val report = reports.find { it.id == reportId }
    var content by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(fileKind) }
    var isLoading by remember { mutableStateOf(true) }

    val kind = runCatching { OOMReportFile.Kind.valueOf(fileKind) }.getOrNull()

    LaunchedEffect(report, kind) {
        if (report != null && kind != null) {
            withContext(Dispatchers.IO) {
                val file = OOMReportManager.availableFiles(report).find { it.kind == kind }
                if (file != null) {
                    displayName = file.displayName
                    content = OOMReportManager.loadFileContent(file)
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (content.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.report_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

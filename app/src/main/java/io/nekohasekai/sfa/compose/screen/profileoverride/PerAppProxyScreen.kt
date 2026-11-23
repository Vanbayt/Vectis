package io.nekohasekai.sfa.compose.screen.profileoverride

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.base.UiEvent
import io.nekohasekai.sfa.compose.base.rememberApplyServiceChangeNotifier
import io.nekohasekai.sfa.compose.shared.AppSelectionCard
import io.nekohasekai.sfa.compose.shared.PackageCache
import io.nekohasekai.sfa.compose.shared.SortMode
import io.nekohasekai.sfa.compose.shared.buildDisplayPackages
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.ktx.clipboardText
import io.nekohasekai.sfa.vendor.PackageQueryManager
import io.nekohasekai.sfa.vendor.PrivilegedAccessRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile

private data class LoadResult(val proxyMode: Int, val packages: List<PackageCache>, val selectedUids: Set<Int>)

private data class ScanProgress(val current: Int, val max: Int)

private sealed class ScanResult {
    data object Empty : ScanResult()
    data class Found(val apps: Map<String, PackageCache>) : ScanResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppProxyScreen(
    onBack: () -> Unit,
    serviceStatus: Status = Status.Stopped,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val notifyApplyChange = rememberApplyServiceChangeNotifier(serviceStatus)

    var perAppProxyEnabled by remember { mutableStateOf(Settings.perAppProxyEnabled) }
    var autoPauseEnabled by remember { mutableStateOf(Settings.autoPauseOnExcludedApps) }
    var pendingEnableAutoPause by remember { mutableStateOf(false) }
    var showUsageStatsDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val a11yActive = io.nekohasekai.sfa.bg.AppForegroundWatcher.isAccessibilityServiceEnabled(context)
                if (pendingEnableAutoPause && a11yActive) {
                    autoPauseEnabled = true
                    pendingEnableAutoPause = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            Settings.autoPauseOnExcludedApps = true
                        }
                    }
                } else if (!a11yActive && autoPauseEnabled) {
                    autoPauseEnabled = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            Settings.autoPauseOnExcludedApps = false
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var proxyMode by remember { mutableStateOf(Settings.perAppProxyMode) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    var sortReverse by remember { mutableStateOf(false) }
    var hideSystemApps by remember { mutableStateOf(false) }
    var hideOfflineApps by remember { mutableStateOf(true) }
    var hideDisabledApps by remember { mutableStateOf(true) }
    var filterOnlySelected by rememberSaveable { mutableStateOf(false) }

    var packages by remember { mutableStateOf<List<PackageCache>>(emptyList()) }
    var selectedUids by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentPackages by remember { mutableStateOf<List<PackageCache>>(emptyList()) }

    var showOptionsSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }

    fun buildPackageList(newUids: Set<Int>): Set<String> = newUids.mapNotNull { uid ->
        packages.find { it.uid == uid }?.packageName
    }.toSet()

    fun saveSelectedApplications(newUids: Set<Int>) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                Settings.perAppProxyList = buildPackageList(newUids)
                if (newUids.isNotEmpty()) {
                    Settings.perAppProxyEnabled = true
                }
            }
            if (newUids.isNotEmpty()) {
                perAppProxyEnabled = true
            }
            notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
        }
    }

    fun postSaveSelectedApplications(newUids: Set<Int>) {
        selectedUids = newUids
        saveSelectedApplications(newUids)
    }

    fun applyRussianPreset(presetPackages: Set<String>) {
        val newSelected = packages.mapNotNull { packageCache ->
            if (presetPackages.contains(packageCache.packageName)) {
                packageCache.uid
            } else {
                null
            }
        }.toSet()
        val packageNames = buildPackageList(newSelected)
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                Settings.perAppProxyList = packageNames
                Settings.perAppProxyEnabled = true
            }
            selectedUids = newSelected
            perAppProxyEnabled = true
            notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
        }
        Toast.makeText(
            context,
            context.getString(R.string.per_app_preset_applied, newSelected.size),
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun toggleSelection(packageCache: PackageCache, selected: Boolean) {
        val newSelected = if (selected) {
            selectedUids + packageCache.uid
        } else {
            selectedUids - packageCache.uid
        }
        if (newSelected == selectedUids) return
        selectedUids = newSelected
        saveSelectedApplications(newSelected)
    }

    // Filtered & sorted display list computed purely in background on Dispatchers.Default
    LaunchedEffect(
        packages,
        selectedUids,
        searchQuery,
        filterOnlySelected,
        hideSystemApps,
        hideOfflineApps,
        hideDisabledApps,
        sortMode,
        sortReverse,
    ) {
        if (packages.isEmpty()) {
            currentPackages = emptyList()
            return@LaunchedEffect
        }
        val computed = withContext(Dispatchers.Default) {
            val baseList = buildDisplayPackages(
                packages = packages,
                selectedUids = selectedUids,
                selectedFirst = true,
                hideSystemApps = hideSystemApps,
                hideOfflineApps = hideOfflineApps,
                hideDisabledApps = hideDisabledApps,
                sortMode = sortMode,
                sortReverse = sortReverse,
            )
            val selectionFiltered = if (filterOnlySelected) {
                baseList.filter { selectedUids.contains(it.uid) }
            } else {
                baseList
            }
            if (searchQuery.isBlank()) {
                selectionFiltered
            } else {
                val q = searchQuery.trim()
                selectionFiltered.filter {
                    it.applicationLabel.contains(q, ignoreCase = true) ||
                        it.packageName.contains(q, ignoreCase = true) ||
                        it.uid.toString().contains(q)
                }
            }
        }
        currentPackages = computed
    }

    fun startScan() {
        if (scanProgress != null) return
        coroutineScope.launch {
            val scanPackages = withContext(Dispatchers.IO) {
                packages.filter { it.isSystem || it.isOffline }
            }
            scanProgress = ScanProgress(0, scanPackages.size)
            val foundApps = withContext(Dispatchers.Default) {
                mutableMapOf<String, PackageCache>().also { found ->
                    val progressInt = AtomicInteger()
                    scanPackages.map { packageCache ->
                        async {
                            if (PerAppProxyScanner.scanChinaPackage(packageCache.info)) {
                                found[packageCache.packageName] = packageCache
                            }
                            val nextValue = progressInt.incrementAndGet()
                            withContext(Dispatchers.Main) {
                                scanProgress = ScanProgress(nextValue, scanPackages.size)
                            }
                        }
                    }.awaitAll()
                }
            }
            scanProgress = null
            scanResult = if (foundApps.isEmpty()) ScanResult.Empty else ScanResult.Found(foundApps)
        }
    }

    // Initial load: parse labels in parallel on Dispatchers.IO to guarantee ZERO UI freeze
    LaunchedEffect(Unit) {
        isLoading = true
        val packageManagerFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES or
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_PERMISSIONS or PackageManager.GET_UNINSTALLED_PACKAGES or
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
            }
        val retryFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_PERMISSIONS
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_UNINSTALLED_PACKAGES or PackageManager.GET_PERMISSIONS
            }
        val loadResult = withContext(Dispatchers.IO) {
            try {
                val mode = if (Settings.perAppProxyMode == Settings.PER_APP_PROXY_INCLUDE) {
                    Settings.PER_APP_PROXY_INCLUDE
                } else {
                    Settings.PER_APP_PROXY_EXCLUDE
                }
                val installedPackages = PackageQueryManager.getInstalledPackages(packageManagerFlags, retryFlags)
                val packageManager = context.packageManager
                val packageCaches = installedPackages.mapNotNull { packageInfo ->
                    if (packageInfo.packageName == context.packageName) return@mapNotNull null
                    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                    val label = appInfo.loadLabel(packageManager).toString()
                    PackageCache(packageInfo, appInfo, packageManager, label)
                }
                val selectedPackageNames = Settings.perAppProxyList.toMutableSet()
                val selectedUidSet = packageCaches.mapNotNull { packageCache ->
                    if (selectedPackageNames.contains(packageCache.packageName)) {
                        packageCache.uid
                    } else {
                        null
                    }
                }.toSet()
                LoadResult(mode, packageCaches, selectedUidSet)
            } catch (_: PrivilegedAccessRequiredException) {
                null
            }
        }
        if (loadResult == null) {
            Toast.makeText(
                context,
                R.string.privileged_access_required,
                Toast.LENGTH_LONG,
            ).show()
            onBack()
            return@LaunchedEffect
        }
        proxyMode = loadResult.proxyMode
        packages = loadResult.packages
        selectedUids = loadResult.selectedUids
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.per_app_proxy),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (perAppProxyEnabled) {
                                if (proxyMode == Settings.PER_APP_PROXY_INCLUDE) {
                                    "Режим: Только выбранные (${selectedUids.size})"
                                } else {
                                    "Режим: Исключения (${selectedUids.size})"
                                }
                            } else {
                                stringResource(R.string.disabled)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                actions = {
                    // Master Switch in TopAppBar
                    Switch(
                        checked = perAppProxyEnabled,
                        onCheckedChange = { newChecked ->
                            perAppProxyEnabled = newChecked
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    Settings.perAppProxyEnabled = newChecked
                                }
                                notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Параметры разделения",
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
                .padding(paddingValues),
        ) {
            // Sticky Filter & Search Bar Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )

                val installedPackageNames = remember(packages) { packages.map { it.packageName }.toSet() }
                val bankingCount = remember(installedPackageNames) { RussianAppPreset.bankingPackages.count { it in installedPackageNames } }
                val gosuslugiCount = remember(installedPackageNames) { RussianAppPreset.gosuslugiPackages.count { it in installedPackageNames } }
                val marketplaceCount = remember(installedPackageNames) { RussianAppPreset.marketplacePackages.count { it in installedPackageNames } }
                val allRfCount = remember(installedPackageNames) { RussianAppPreset.allRussianPackages.count { it in installedPackageNames } }
                val yandexVkCount = remember(installedPackageNames) { (RussianAppPreset.yandexPackages + RussianAppPreset.vkPackages).count { it in installedPackageNames } }
                val mediaCount = remember(installedPackageNames) { RussianAppPreset.mediaPackages.count { it in installedPackageNames } }

                // Horizontal Carousel of Mode, Quick Filter & RF Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Mode Toggle Chip
                    item {
                        val isExclude = proxyMode == Settings.PER_APP_PROXY_EXCLUDE
                        FilterChip(
                            selected = true,
                            onClick = {
                                val newMode = if (isExclude) Settings.PER_APP_PROXY_INCLUDE else Settings.PER_APP_PROXY_EXCLUDE
                                proxyMode = newMode
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        Settings.perAppProxyMode = newMode
                                    }
                                    notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
                                }
                            },
                            label = {
                                Text(if (isExclude) "Исключать выбранные" else "Только выбранные")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isExclude) Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }

                    // Quick Filter: Selected Only
                    item {
                        FilterChip(
                            selected = filterOnlySelected,
                            onClick = { filterOnlySelected = !filterOnlySelected },
                            label = { Text("Выбранные (${selectedUids.size})") },
                            leadingIcon = {
                                if (filterOnlySelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                } else null
                            },
                            shape = CircleShape,
                        )
                    }

                    // Presets with dynamic counts and clean icons
                    item {
                        AssistChip(
                            onClick = { applyRussianPreset(RussianAppPreset.bankingPackages) },
                            label = { Text(if (bankingCount > 0) "Банки ($bankingCount)" else "Банки") },
                            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }

                    item {
                        AssistChip(
                            onClick = { applyRussianPreset(RussianAppPreset.gosuslugiPackages) },
                            label = { Text(if (gosuslugiCount > 0) "Госуслуги ($gosuslugiCount)" else "Госуслуги") },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }

                    item {
                        AssistChip(
                            onClick = { applyRussianPreset(RussianAppPreset.marketplacePackages) },
                            label = { Text(if (marketplaceCount > 0) "Маркетплейсы ($marketplaceCount)" else "Маркетплейсы") },
                            leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }

                    item {
                        AssistChip(
                            onClick = { applyRussianPreset(RussianAppPreset.allRussianPackages) },
                            label = { Text(if (allRfCount > 0) "Все РФ ($allRfCount)" else "Все РФ") },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }

                    item {
                        AssistChip(
                            onClick = { applyRussianPreset(RussianAppPreset.yandexPackages + RussianAppPreset.vkPackages) },
                            label = { Text(if (yandexVkCount > 0) "Яндекс & VK ($yandexVkCount)" else "Яндекс & VK") },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }

                    item {
                        AssistChip(
                            onClick = { applyRussianPreset(RussianAppPreset.mediaPackages) },
                            label = { Text(if (mediaCount > 0) "Медиа РФ ($mediaCount)" else "Медиа РФ") },
                            shape = CircleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }

                    item {
                        val hasActiveFilters = hideSystemApps || hideOfflineApps || hideDisabledApps || sortMode != SortMode.NAME || sortReverse
                        FilterChip(
                            selected = hasActiveFilters,
                            onClick = { showFilterSheet = true },
                            label = { Text("Фильтр и сортировка") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = CircleShape,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(44.dp))
                        Text(
                            text = "Подготовка списка приложений...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Inactive State Banner when Master switch is turned off
                    if (!perAppProxyEnabled) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        perAppProxyEnabled = true
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                Settings.perAppProxyEnabled = true
                                            }
                                            notifyApplyChange(UiEvent.ApplyServiceChange.Mode.Restart)
                                        }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Разделение трафика выключено",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Весь трафик направляется через VPN. Нажмите, чтобы активировать правила.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Counter Bar & Quick Deselect
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Приложения",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Text("${selectedUids.size} / ${currentPackages.size}")
                                }
                            }

                            if (selectedUids.isNotEmpty()) {
                                TextButton(onClick = { postSaveSelectedApplications(emptySet()) }) {
                                    Text("Снять все")
                                }
                            }
                        }
                    }

                    if (currentPackages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) stringResource(R.string.report_empty) else "Нет приложений",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    items(currentPackages, key = { it.packageName }) { packageCache ->
                        AppSelectionCard(
                            packageCache = packageCache,
                            selected = selectedUids.contains(packageCache.uid),
                            onToggle = { selected -> toggleSelection(packageCache, selected) },
                            enableCopyActions = true,
                            onCopyLabel = { clipboardText = packageCache.applicationLabel },
                            onCopyPackage = { clipboardText = packageCache.packageName },
                            onCopyUid = { clipboardText = packageCache.uid.toString() },
                        )
                    }
                }
            }
        }
    }

    // Parameters ModalBottomSheet
    if (showOptionsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Параметры разделения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                // Smart Auto-Pause
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val isRootMode = Settings.serviceMode == io.nekohasekai.sfa.constant.ServiceMode.ROOT_TUN
                        if (isRootMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column {
                                    Text(
                                        text = "Root Native TUN",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Аппаратная изоляция сетевого трафика активна",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val willEnable = !autoPauseEnabled
                                        if (willEnable) {
                                            if (!io.nekohasekai.sfa.bg.AppForegroundWatcher.isAccessibilityServiceEnabled(context)) {
                                                pendingEnableAutoPause = true
                                                showUsageStatsDialog = true
                                            } else {
                                                autoPauseEnabled = true
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        Settings.autoPauseOnExcludedApps = true
                                                    }
                                                }
                                            }
                                        } else {
                                            autoPauseEnabled = false
                                            pendingEnableAutoPause = false
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    Settings.autoPauseOnExcludedApps = false
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = stringResource(R.string.per_app_auto_pause_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = stringResource(R.string.per_app_auto_pause_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = autoPauseEnabled,
                                    onCheckedChange = null,
                                )
                            }
                        }
                    }
                }

                // Batch & Clipboard Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showOptionsSheet = false
                                    postSaveSelectedApplications(packages.map { it.uid }.toSet())
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.per_app_proxy_select_all), style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showOptionsSheet = false
                                    postSaveSelectedApplications(emptySet())
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.per_app_proxy_select_none), style = MaterialTheme.typography.bodyMedium)
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showOptionsSheet = false
                                    val packageNames = clipboardText?.split("\n")?.distinct()?.filter { it.isNotBlank() }
                                    if (packageNames.isNullOrEmpty()) {
                                        Toast.makeText(context, R.string.toast_clipboard_empty, Toast.LENGTH_SHORT).show()
                                    } else {
                                        val newSelected = packages.mapNotNull { packageCache ->
                                            if (packageNames.contains(packageCache.packageName)) packageCache.uid else null
                                        }.toSet()
                                        postSaveSelectedApplications(newSelected)
                                        Toast.makeText(context, R.string.toast_imported_from_clipboard, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.per_app_proxy_import), style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showOptionsSheet = false
                                    val packageList = packages.mapNotNull { packageCache ->
                                        if (selectedUids.contains(packageCache.uid)) packageCache.packageName else null
                                    }
                                    clipboardText = packageList.joinToString("\n")
                                    Toast.makeText(context, R.string.toast_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.per_app_proxy_export), style = MaterialTheme.typography.bodyMedium)
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showOptionsSheet = false
                                    startScan()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ManageSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.per_app_proxy_scan_china_apps), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // Filter & Sort BottomSheet
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Фильтры и сортировка",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                // Sort Section
                Text(
                    text = "Сортировка",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = sortMode == SortMode.NAME,
                        onClick = { sortMode = SortMode.NAME },
                        label = { Text("По имени") },
                        shape = CircleShape,
                    )
                    FilterChip(
                        selected = sortMode == SortMode.PACKAGE_NAME,
                        onClick = { sortMode = SortMode.PACKAGE_NAME },
                        label = { Text("По пакету") },
                        shape = CircleShape,
                    )
                    FilterChip(
                        selected = sortMode == SortMode.INSTALL_TIME,
                        onClick = { sortMode = SortMode.INSTALL_TIME },
                        label = { Text("По дате") },
                        shape = CircleShape,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { sortReverse = !sortReverse }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("В обратном порядке", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = sortReverse, onCheckedChange = { sortReverse = it })
                }

                HorizontalDivider()

                // Filter Toggles
                Text(
                    text = "Фильтрация списка",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { hideSystemApps = !hideSystemApps }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.per_app_proxy_hide_system_apps), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hideSystemApps, onCheckedChange = { hideSystemApps = it })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { hideOfflineApps = !hideOfflineApps }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.per_app_proxy_hide_offline_apps), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hideOfflineApps, onCheckedChange = { hideOfflineApps = it })
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { hideDisabledApps = !hideDisabledApps }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.per_app_proxy_hide_disabled_apps), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hideDisabledApps, onCheckedChange = { hideDisabledApps = it })
                }
            }
        }
    }

    // Usage Stats / Accessibility Dialog
    if (showUsageStatsDialog) {
        AlertDialog(
            onDismissRequest = {
                showUsageStatsDialog = false
                pendingEnableAutoPause = false
            },
            title = { Text(stringResource(R.string.per_app_auto_pause_permission_dialog_title)) },
            text = { Text(stringResource(R.string.per_app_auto_pause_permission_dialog_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUsageStatsDialog = false
                        try {
                            val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(AndroidSettings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.per_app_auto_pause_permission_btn))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUsageStatsDialog = false
                        pendingEnableAutoPause = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // Scan Progress Dialog
    scanProgress?.let { progress ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.per_app_proxy_scan_china_apps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(
                        progress = { if (progress.max > 0) progress.current.toFloat() / progress.max else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${progress.current} / ${progress.max}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // Scan Result Dialog
    scanResult?.let { result ->
        when (result) {
            is ScanResult.Empty -> {
                AlertDialog(
                    onDismissRequest = { scanResult = null },
                    title = { Text(stringResource(R.string.message_scan_app_no_apps_found)) },
                    confirmButton = {
                        TextButton(onClick = { scanResult = null }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
                )
            }

            is ScanResult.Found -> {
                val foundApps = result.apps
                AlertDialog(
                    onDismissRequest = { scanResult = null },
                    title = { Text(stringResource(R.string.message_scan_app_found, foundApps.size)) },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(foundApps.values.toList()) { app ->
                                Text(
                                    text = "• ${app.applicationLabel} (${app.packageName})",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val newSelected = selectedUids + foundApps.values.map { it.uid }
                                postSaveSelectedApplications(newSelected)
                                scanResult = null
                            },
                        ) {
                            Text(stringResource(R.string.per_app_proxy_select))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                val newSelected = selectedUids - foundApps.values.map { it.uid }.toSet()
                                postSaveSelectedApplications(newSelected)
                                scanResult = null
                            },
                        ) {
                            Text(stringResource(R.string.action_deselect))
                        }
                    },
                )
            }
        }
    }
}

object PerAppProxyScanner {
    fun scanChinaPackage(packageInfo: PackageInfo): Boolean {
        val applicationInfo = packageInfo.applicationInfo ?: return false
        val sourceDir = File(applicationInfo.publicSourceDir ?: applicationInfo.sourceDir)
        if (!sourceDir.exists()) return false

        try {
            ZipFile(sourceDir).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("classes") && entry.name.endsWith(".dex")) {
                        zip.getInputStream(entry).use { inputStream ->
                            val dexFile = DexBackedDexFile.fromInputStream(null, inputStream)
                            for (type in dexFile.classes) {
                                if (type.type.startsWith("Lcom/tencent/mm/")) return true
                                if (type.type.startsWith("Lcom/tencent/mobileqq/")) return true
                                if (type.type.startsWith("Lcom/alipay/")) return true
                                if (type.type.startsWith("Lcom/ss/android/")) return true
                                if (type.type.startsWith("Lcom/bytedance/")) return true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return false
        }
        return false
    }

    suspend fun scanAllChinaApps(): Set<String> = withContext(Dispatchers.IO) {
        val packageManager = Application.application.packageManager
        val packages = packageManager.getInstalledPackages(0)
        val chinaPackages = mutableSetOf<String>()
        for (pkg in packages) {
            if (scanChinaPackage(pkg)) {
                chinaPackages.add(pkg.packageName)
            }
        }
        chinaPackages
    }
}

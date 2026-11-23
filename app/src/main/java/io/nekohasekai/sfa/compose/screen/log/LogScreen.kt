package io.nekohasekai.sfa.compose.screen.log

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compat.WindowSizeClassCompat
import io.nekohasekai.sfa.compat.isWidthAtLeastBreakpointCompat
import io.nekohasekai.sfa.constant.Status
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    title: String? = null,
    viewModel: LogViewerViewModel? = null,
    showPause: Boolean = true,
    showClear: Boolean = true,
    showStatusInfo: Boolean = true,
    emptyMessage: String? = null,
    saveFilePrefix: String = "logs",
    onBack: (() -> Unit)? = null,
) {
    val resolvedViewModel = viewModel ?: viewModel<LogViewModel>()
    val uiState by resolvedViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isTablet = windowSizeClass.isWidthAtLeastBreakpointCompat(WindowSizeClassCompat.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val resolvedTitle = title ?: stringResource(R.string.title_log)
    val emptyStateMessage = emptyMessage ?: stringResource(R.string.privilege_settings_hook_logs_empty)

    // Handle back press in selection mode
    androidx.activity.compose.BackHandler(enabled = uiState.isSelectionMode) {
        resolvedViewModel.clearSelection()
    }

    // Track if user is at the bottom of the list
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        }
    }

    var lastSeenLogCount by remember { mutableIntStateOf(uiState.logs.size) }
    LaunchedEffect(isAtBottom, uiState.logs.size) {
        if (isAtBottom) {
            lastSeenLogCount = uiState.logs.size
        }
    }
    val unreadNewLogsCount = remember(isAtBottom, uiState.logs.size, lastSeenLogCount) {
        if (isAtBottom) 0 else (uiState.logs.size - lastSeenLogCount).coerceAtLeast(0)
    }

    // Re-enable auto-scroll when user reaches bottom
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            resolvedViewModel.setAutoScrollEnabled(true)
        }
    }

    // Detect user manual scroll to disable auto-scroll
    LaunchedEffect(listState) {
        var dragStartIndex: Int? = null
        var dragStartOffset: Int? = null

        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    dragStartIndex = listState.firstVisibleItemIndex
                    dragStartOffset = listState.firstVisibleItemScrollOffset
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    if (dragStartIndex != null && dragStartOffset != null) {
                        val currentIndex = listState.firstVisibleItemIndex
                        val currentOffset = listState.firstVisibleItemScrollOffset

                        val scrolledUp =
                            if (dragStartIndex != currentIndex) {
                                dragStartIndex!! > currentIndex
                            } else {
                                dragStartOffset!! > currentOffset
                            }

                        if (scrolledUp) {
                            resolvedViewModel.setAutoScrollEnabled(false)
                        }

                        dragStartIndex = null
                        dragStartOffset = null
                    }
                }
            }
        }
    }

    // Handle scroll to bottom requests from ViewModel
    val scrollToBottomTrigger by resolvedViewModel.scrollToBottomTrigger.collectAsState()
    LaunchedEffect(scrollToBottomTrigger) {
        if (scrollToBottomTrigger > 0 && uiState.logs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logs.size - 1)
        }
    }

    // Update service status in ViewModel
    LaunchedEffect(serviceStatus) {
        if (showStatusInfo) {
            resolvedViewModel.updateServiceStatus(serviceStatus)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolvedTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { resolvedViewModel.toggleSearch() }) {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { resolvedViewModel.togglePause() }) {
                        Icon(
                            imageVector = if (uiState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = stringResource(R.string.content_description_pause_logs)
                        )
                    }
                    IconButton(onClick = { resolvedViewModel.requestClearLogs() }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.clear_logs))
                    }
                    IconButton(onClick = { resolvedViewModel.toggleOptionsMenu() }) {
                        Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {




            // Show selection mode bar
            if (uiState.isSelectionMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { resolvedViewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.content_description_exit_selection_mode),
                                )
                            }
                            Text(
                                text =
                                stringResource(
                                    R.string.selected_count,
                                    uiState.selectedLogIndices.size,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    val selectedText = resolvedViewModel.getSelectedLogsText()
                                    if (selectedText.isNotEmpty()) {
                                        val clipLabel = resolvedTitle
                                        val clip = ClipData.newPlainText(clipLabel, selectedText)
                                        Application.clipboard.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.copied_to_clipboard),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        resolvedViewModel.clearSelection()
                                    }
                                },
                                enabled = uiState.selectedLogIndices.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.content_description_copy_selected),
                                )
                            }
                        }
                    }
                }
            } else {
                // Quick FilterChips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // All
                    FilterChip(
                        selected = uiState.filterLogLevel == LogLevel.Default,
                        onClick = { resolvedViewModel.setLogLevel(LogLevel.Default) },
                        label = { Text(stringResource(R.string.log_level_all)) },
                        leadingIcon = if (uiState.filterLogLevel == LogLevel.Default) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Info
                    FilterChip(
                        selected = uiState.filterLogLevel == LogLevel.INFO,
                        onClick = {
                            resolvedViewModel.setLogLevel(
                                if (uiState.filterLogLevel == LogLevel.INFO) LogLevel.Default else LogLevel.INFO
                            )
                        },
                        label = { Text(stringResource(R.string.log_level_info)) },
                        leadingIcon = if (uiState.filterLogLevel == LogLevel.INFO) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Warn
                    FilterChip(
                        selected = uiState.filterLogLevel == LogLevel.WARNING,
                        onClick = {
                            resolvedViewModel.setLogLevel(
                                if (uiState.filterLogLevel == LogLevel.WARNING) LogLevel.Default else LogLevel.WARNING
                            )
                        },
                        label = { Text(stringResource(R.string.log_level_warn)) },
                        leadingIcon = if (uiState.filterLogLevel == LogLevel.WARNING) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Error
                    FilterChip(
                        selected = uiState.filterLogLevel == LogLevel.ERROR,
                        onClick = {
                            resolvedViewModel.setLogLevel(
                                if (uiState.filterLogLevel == LogLevel.ERROR) LogLevel.Default else LogLevel.ERROR
                            )
                        },
                        label = { Text(stringResource(R.string.log_level_error)) },
                        leadingIcon = if (uiState.filterLogLevel == LogLevel.ERROR) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Debug
                    FilterChip(
                        selected = uiState.filterLogLevel == LogLevel.DEBUG,
                        onClick = {
                            resolvedViewModel.setLogLevel(
                                if (uiState.filterLogLevel == LogLevel.DEBUG) LogLevel.Default else LogLevel.DEBUG
                            )
                        },
                        label = { Text(stringResource(R.string.log_level_debug)) },
                        leadingIcon = if (uiState.filterLogLevel == LogLevel.DEBUG) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            // Show search bar with animation
            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter =
                expandVertically(
                    animationSpec = tween(300),
                ) +
                    fadeIn(
                        animationSpec = tween(300),
                    ),
                exit =
                shrinkVertically(
                    animationSpec = tween(300),
                ) +
                    fadeOut(
                        animationSpec = tween(300),
                    ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                ) {
                    val focusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { resolvedViewModel.updateSearchQuery(it) },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.search_logs_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.search),
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { resolvedViewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.content_description_clear_search),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions =
                        KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                            },
                        ),
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = uiState.errorTitle ?: "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (uiState.logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (showStatusInfo) {
                                when (serviceStatus) {
                                    Status.Started -> stringResource(R.string.status_started)
                                    Status.Starting -> stringResource(R.string.status_starting)
                                    Status.Stopping -> stringResource(R.string.status_stopping)
                                    else -> stringResource(R.string.status_default)
                                }
                            } else {
                                emptyStateMessage
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Log list
                val bottomPadding = when {
                    showStartFab -> 88.dp
                    showStatusBar -> 74.dp
                    else -> 0.dp
                }
                Card(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = bottomPadding),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 20.dp,
                            bottom = 20.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.logs,
                            key = { _, log -> log.id },
                        ) { index, log ->
                            LogItem(
                                annotatedString = log.annotatedString,
                                level = log.entry.level,
                                index = index,
                                isSelected = uiState.selectedLogIndices.contains(index),
                                isSelectionMode = uiState.isSelectionMode,
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        resolvedViewModel.toggleSelectionMode()
                                        resolvedViewModel.toggleLogSelection(index)
                                    }
                                },
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        resolvedViewModel.toggleLogSelection(index)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        } // Close Column

        // Options Menu - Material 3 style
        Box(
            modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp),
        ) {
            var expandedLogLevel by remember { mutableStateOf(false) }
            var expandedSave by remember { mutableStateOf(false) }

            // File save launcher (must be outside DropdownMenu)
            val saveFileLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/plain"),
                    onResult = { uri ->
                        uri?.let {
                            try {
                                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                    val logsText = resolvedViewModel.getAllLogsText()
                                    outputStream.write(logsText.toByteArray())
                                    outputStream.flush()
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.success_logs_saved),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.failed_save_logs, e.message),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )

            DropdownMenu(
                expanded = uiState.isOptionsMenuOpen,
                onDismissRequest = {
                    resolvedViewModel.toggleOptionsMenu()
                    expandedLogLevel = false
                    expandedSave = false
                },
                modifier = Modifier.widthIn(min = 200.dp),
            ) {
                // Log Level section with nested items
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.log_level),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = { expandedLogLevel = !expandedLogLevel },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector =
                            if (expandedLogLevel) {
                                Icons.Rounded.ExpandLess
                            } else {
                                Icons.Rounded.ExpandMore
                            },
                            contentDescription = null,
                        )
                    },
                )

                // Show log levels inline when expanded
                if (expandedLogLevel) {
                    LogLevel.entries.filter { it.priority > 1 }.forEach { level ->
                        DropdownMenuItem(
                            text = {
                                Text(text = level.label)
                            },
                            onClick = {
                                resolvedViewModel.setLogLevel(level)
                                resolvedViewModel.toggleOptionsMenu()
                                expandedLogLevel = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                    if (uiState.filterLogLevel == level) {
                                        Icons.Rounded.RadioButtonChecked
                                    } else {
                                        Icons.Rounded.RadioButtonUnchecked
                                    },
                                    contentDescription =
                                    if (uiState.filterLogLevel == level) {
                                        stringResource(R.string.group_selected_title)
                                    } else {
                                        null
                                    },
                                    tint =
                                    if (uiState.filterLogLevel == level) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(start = 24.dp),
                                )
                            },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Save section with nested items
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = { expandedSave = !expandedSave },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector =
                            if (expandedSave) {
                                Icons.Rounded.ExpandLess
                            } else {
                                Icons.Rounded.ExpandMore
                            },
                            contentDescription = null,
                        )
                    },
                )

                // Show save options inline when expanded
                if (expandedSave) {
                    // Copy to Clipboard
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(R.string.save_to_clipboard))
                        },
                        onClick = {
                            val logsText = resolvedViewModel.getAllLogsText()
                            if (logsText.isNotEmpty()) {
                                val clip =
                                    ClipData.newPlainText(resolvedTitle, logsText)
                                Application.clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.logs_copied_to_clipboard),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.no_logs_to_copy),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            resolvedViewModel.toggleOptionsMenu()
                            expandedSave = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 24.dp),
                            )
                        },
                    )

                    // Save to File
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(R.string.save_to_file))
                        },
                        onClick = {
                            val timestamp =
                                SimpleDateFormat(
                                    "yyyyMMdd_HHmmss",
                                    Locale.getDefault(),
                                ).format(Date())
                            saveFileLauncher.launch("${saveFilePrefix}_$timestamp.txt")
                            resolvedViewModel.toggleOptionsMenu()
                            expandedSave = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 24.dp),
                            )
                        },
                    )

                    // Share as File
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(R.string.menu_share))
                        },
                        onClick = {
                            val logsText = resolvedViewModel.getAllLogsText()
                            if (logsText.isNotEmpty()) {
                                try {
                                    val logsDir =
                                        File(context.cacheDir, "logs").also { it.mkdirs() }
                                    val timestamp =
                                        SimpleDateFormat(
                                            "yyyyMMdd_HHmmss",
                                            Locale.getDefault(),
                                        ).format(Date())
                                    val logFile = File(logsDir, "${saveFilePrefix}_$timestamp.txt")
                                    logFile.writeText(logsText)

                                    val uri =
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.cache",
                                            logFile,
                                        )
                                    val shareIntent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.intent_share_logs),
                                        ),
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.failed_share_logs, e.message),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.no_logs_to_share),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            resolvedViewModel.toggleOptionsMenu()
                            expandedSave = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 24.dp),
                            )
                        },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (showClear) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.clear_logs),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            resolvedViewModel.requestClearLogs()
                            resolvedViewModel.toggleOptionsMenu()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }

        // FABs - Hide during selection mode
        val padFabVisible = isTablet && (showStartFab || showStatusBar)
        val fabBottomPadding = when {
            padFabVisible -> 20.dp + 64.dp + 16.dp
            showStartFab -> 88.dp
            showStatusBar -> 74.dp
            else -> 16.dp
        }
        val fabEndPadding = if (isTablet) 20.dp else 16.dp
        Column(
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = fabBottomPadding, end = fabEndPadding, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Scroll to bottom FAB with unread new logs badge
            AnimatedVisibility(
                visible = !isAtBottom && !uiState.isSelectionMode && uiState.logs.isNotEmpty(),
                enter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) scaleIn() else fadeIn(),
                exit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) scaleOut() else fadeOut(),
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNewLogsCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(
                                    text = if (unreadNewLogsCount > 99) "99+" else unreadNewLogsCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    },
                ) {
                    FloatingActionButton(
                        onClick = {
                            resolvedViewModel.scrollToBottom()
                            lastSeenLogCount = uiState.logs.size
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.content_description_scroll_to_bottom),
                        )
                    }
                }
            }
        }
    }
}
}






@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItem(
    annotatedString: androidx.compose.ui.text.AnnotatedString,
    level: LogLevel = LogLevel.Default,
    index: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        level == LogLevel.ERROR || level == LogLevel.FATAL || level == LogLevel.PANIC ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        level == LogLevel.WARNING ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val cardBorder = when {
        isSelected -> CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            ),
        )
        level == LogLevel.ERROR || level == LogLevel.FATAL || level == LogLevel.PANIC ->
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                ),
            )
        level == LogLevel.WARNING ->
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                ),
            )
        else -> null
    }

    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = cardBorder,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription =
                    if (isSelected) {
                        stringResource(R.string.group_selected_title)
                    } else {
                        stringResource(R.string.not_selected)
                    },
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = annotatedString,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (isSelectionMode) 4.dp else 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
            )
        }
    }
}



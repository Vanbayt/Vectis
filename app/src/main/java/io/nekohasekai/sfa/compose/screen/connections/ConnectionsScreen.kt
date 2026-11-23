package io.nekohasekai.sfa.compose.screen.connections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compat.LazyColumnCompat
import io.nekohasekai.sfa.compat.rememberOverscrollEffectCompat
import io.nekohasekai.sfa.compose.model.Connection
import io.nekohasekai.sfa.compose.model.ConnectionSort
import io.nekohasekai.sfa.compose.model.ConnectionStateFilter
import io.nekohasekai.sfa.compose.util.rememberSheetDismissFromContentOnlyIfGestureStartedAtTopModifier
import io.nekohasekai.sfa.constant.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsPage(
    serviceStatus: Status,
    viewModel: ConnectionsViewModel = viewModel(),
    asSheet: Boolean = false,
    showTitle: Boolean = true,
    showTopBar: Boolean = false,
    onConnectionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStateMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showConnectionsMenu by remember { mutableStateOf(false) }

    val headerRowModifier =
        if (asSheet) {
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        }

    val headerContent: @Composable () -> Unit = {
        Row(
            modifier = headerRowModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showTitle || showTopBar) {
                Text(
                    text = stringResource(R.string.title_connections),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box {
                FilterChip(
                    selected = uiState.stateFilter != ConnectionStateFilter.Active,
                    onClick = { showStateMenu = true },
                    label = {
                        Text(
                            when (uiState.stateFilter) {
                                ConnectionStateFilter.All -> stringResource(R.string.connection_state_all)
                                ConnectionStateFilter.Active -> stringResource(R.string.connection_state_active)
                                ConnectionStateFilter.Closed -> stringResource(R.string.connection_state_closed)
                            },
                        )
                    },
                )

                DropdownMenu(
                    expanded = showStateMenu,
                    onDismissRequest = { showStateMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_state_all)) },
                        onClick = {
                            viewModel.setStateFilter(ConnectionStateFilter.All)
                            showStateMenu = false
                        },
                        leadingIcon = {
                            if (uiState.stateFilter == ConnectionStateFilter.All) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_state_active)) },
                        onClick = {
                            viewModel.setStateFilter(ConnectionStateFilter.Active)
                            showStateMenu = false
                        },
                        leadingIcon = {
                            if (uiState.stateFilter == ConnectionStateFilter.Active) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_state_closed)) },
                        onClick = {
                            viewModel.setStateFilter(ConnectionStateFilter.Closed)
                            showStateMenu = false
                        },
                        leadingIcon = {
                            if (uiState.stateFilter == ConnectionStateFilter.Closed) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                    )
                }
            }

            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Rounded.SwapVert, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_sort_date)) },
                        onClick = {
                            viewModel.setSort(ConnectionSort.ByDate)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (uiState.sort == ConnectionSort.ByDate) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_sort_traffic)) },
                        onClick = {
                            viewModel.setSort(ConnectionSort.ByTraffic)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (uiState.sort == ConnectionSort.ByTraffic) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_sort_traffic_total)) },
                        onClick = {
                            viewModel.setSort(ConnectionSort.ByTrafficTotal)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (uiState.sort == ConnectionSort.ByTrafficTotal) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                    )
                }
            }

            Box {
                IconButton(onClick = { showConnectionsMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showConnectionsMenu,
                    onDismissRequest = { showConnectionsMenu = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (uiState.isSearchActive) {
                                    stringResource(R.string.close_search)
                                } else {
                                    stringResource(R.string.search)
                                },
                            )
                        },
                        onClick = {
                            viewModel.toggleSearch()
                            showConnectionsMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.connection_close_all)) },
                        onClick = {
                            viewModel.closeAllConnections()
                            showConnectionsMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                        },
                        enabled = uiState.connections.any { it.isActive },
                    )
                }
            }
        }
    }

    if (asSheet) {
        ConnectionsScreen(
            serviceStatus = serviceStatus,
            viewModel = viewModel,
            onConnectionClick = { connection -> onConnectionClick(connection.id) },
            listHeaderContent = headerContent,
            asSheet = true,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
        ) {
            headerContent()
            ConnectionsScreen(
                serviceStatus = serviceStatus,
                viewModel = viewModel,
                onConnectionClick = { connection -> onConnectionClick(connection.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDetailsRoute(
    connectionId: String,
    serviceStatus: Status,
    viewModel: ConnectionsViewModel = viewModel(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val connection =
        uiState.allConnections.find { it.id == connectionId }
            ?: uiState.connections.find { it.id == connectionId }
    var cachedConnection by remember { mutableStateOf(connection) }
    if (connection != null) {
        cachedConnection = connection
    } else if (cachedConnection?.isActive == true) {
        cachedConnection = cachedConnection?.copy(closedAt = System.currentTimeMillis())
    }

    LaunchedEffect(Unit) {
        viewModel.setVisible(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setVisible(false)
        }
    }

    LaunchedEffect(serviceStatus) {
        viewModel.updateServiceStatus(serviceStatus)
    }

    if (cachedConnection == null) {
        LaunchedEffect(connectionId) {
            onBack()
        }
        Box(modifier = modifier.fillMaxSize())
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.connection_details), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                            )
                        }
                    },
                    actions = {
                        if (cachedConnection?.isActive == true) {
                            IconButton(onClick = { viewModel.closeConnection(connectionId) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.connection_close),
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
            ConnectionDetailsScreen(
                connection = cachedConnection!!,
                onBack = onBack,
                onClose = { viewModel.closeConnection(connectionId) },
                modifier = modifier.padding(paddingValues),
                showHeader = false,
            )
        }
    }
}

@Composable
fun ConnectionsScreen(
    serviceStatus: Status,
    viewModel: ConnectionsViewModel = viewModel(),
    onConnectionClick: (Connection) -> Unit = {},
    listHeaderContent: (@Composable () -> Unit)? = null,
    asSheet: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setVisible(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setVisible(false)
        }
    }

    LaunchedEffect(serviceStatus) {
        viewModel.updateServiceStatus(serviceStatus)
    }

    val lazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    if (asSheet) {
        val sheetSwipeToDismissModifier =
            rememberSheetDismissFromContentOnlyIfGestureStartedAtTopModifier {
                lazyListState.firstVisibleItemIndex == 0 &&
                    lazyListState.firstVisibleItemScrollOffset == 0
            }
        LazyColumnCompat(
            modifier =
            modifier
                .fillMaxSize()
                .then(sheetSwipeToDismissModifier),
            state = lazyListState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = null,
        ) {
            if (listHeaderContent != null) {
                item(key = "connections_list_header") {
                    listHeaderContent()
                }
            }

            item(key = "connections_hero_card") {
                val allList = if (uiState.allConnections.isNotEmpty()) uiState.allConnections else uiState.connections
                val activeCount = allList.count { it.isActive }
                val totalCount = allList.size
                val uploadTotal = allList.sumOf { it.uploadTotal }
                val downloadTotal = allList.sumOf { it.downloadTotal }
                ConnectionsHeroSummaryCard(
                    activeCount = activeCount,
                    totalCount = totalCount,
                    uploadTotal = uploadTotal,
                    downloadTotal = downloadTotal,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            item(key = "connections_search") {
                AnimatedVisibility(
                    visible = uiState.isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    OutlinedTextField(
                        value = uiState.searchText,
                        onValueChange = { viewModel.setSearchText(it) },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.search_connections)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchText("") }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    item(key = "connections_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                uiState.connections.isEmpty() -> {
                    item(key = "connections_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.empty_connections),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    items(
                        items = uiState.connections,
                        key = { it.id },
                    ) { connection ->
                        ConnectionItem(
                            connection = connection,
                            onClick = { onConnectionClick(connection) },
                            onClose = { viewModel.closeConnection(connection.id) },
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            val allList = if (uiState.allConnections.isNotEmpty()) uiState.allConnections else uiState.connections
            val activeCount = allList.count { it.isActive }
            val totalCount = allList.size
            val uploadTotal = allList.sumOf { it.uploadTotal }
            val downloadTotal = allList.sumOf { it.downloadTotal }
            ConnectionsHeroSummaryCard(
                activeCount = activeCount,
                totalCount = totalCount,
                uploadTotal = uploadTotal,
                downloadTotal = downloadTotal,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )

            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                OutlinedTextField(
                    value = uiState.searchText,
                    onValueChange = { viewModel.setSearchText(it) },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.search_connections)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchText("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.connections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.empty_connections),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    val bounceBlockingConnection = rememberBounceBlockingNestedScrollConnection(lazyListState)
                    LazyColumnCompat(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(bounceBlockingConnection),
                        state = lazyListState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        overscrollEffect = rememberOverscrollEffectCompat(),
                    ) {
                        items(
                            items = uiState.connections,
                            key = { it.id },
                        ) { connection ->
                            ConnectionItem(
                                connection = connection,
                                onClick = { onConnectionClick(connection) },
                                onClose = { viewModel.closeConnection(connection.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberBounceBlockingNestedScrollConnection(lazyListState: LazyListState): NestedScrollConnection = remember(lazyListState) {
    object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = if (available.y < 0) available else Offset.Zero

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = if (available.y < 0) available else Velocity.Zero
    }
}

@Composable
fun ConnectionsHeroSummaryCard(
    activeCount: Int,
    totalCount: Int,
    uploadTotal: Long,
    downloadTotal: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Active / Total Connections
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(4.dp),
                            ),
                    )
                    Text(
                        text = "$activeCount / $totalCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.connection_state_active),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Downlink
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = Libbox.formatBytes(downloadTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.connection_downlink),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Uplink
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = Libbox.formatBytes(uploadTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.connection_uplink),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

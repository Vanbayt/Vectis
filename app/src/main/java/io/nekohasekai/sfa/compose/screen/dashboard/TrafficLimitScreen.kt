package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.nekohasekai.sfa.compose.screen.dashboard.DashboardViewModel
import io.nekohasekai.sfa.compose.shared.PackageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

data class AppTrafficData(
    val packageCache: PackageCache?,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long
)

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f ГБ", bytes.toFloat() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.2f МБ", bytes.toFloat() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.2f КБ", bytes.toFloat() / 1024)
        else -> "$bytes Б"
    }
}

fun checkUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficLimitScreen(navController: NavController, viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isUnlimited = state.trafficLimit <= 0L
    val usedGb = state.trafficUsed.toFloat() / (1024 * 1024 * 1024)
    val limitGb = state.trafficLimit.toFloat() / (1024 * 1024 * 1024)
    val remainingGb = maxOf(0f, limitGb - usedGb)
    val progress = if (isUnlimited) 0f else if (limitGb > 0) (usedGb / limitGb).coerceIn(0f, 1f) else 0f
    val remainingStr = if (isUnlimited) "∞" else String.format("%.1f ГБ", remainingGb)
    val limitStr = if (isUnlimited) "∞" else String.format("%.1f ГБ", limitGb)

    val calendarReset = java.util.Calendar.getInstance()
    val currentDay = calendarReset.get(java.util.Calendar.DAY_OF_MONTH)
    val daysInMonth = calendarReset.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val daysUntilReset = daysInMonth - currentDay + 1
    val resetStr = "Через $daysUntilReset дн."

    val context = LocalContext.current
    var hasUsageAccess by remember { mutableStateOf(checkUsageAccess(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = checkUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var appTrafficList by remember { mutableStateOf<List<AppTrafficData>>(emptyList()) }
    var isLoadingTraffic by remember { mutableStateOf(false) }

    LaunchedEffect(hasUsageAccess) {
        if (hasUsageAccess) {
            isLoadingTraffic = true
            appTrafficList = withContext(Dispatchers.IO) {
                val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
                val pm = context.packageManager

                val buckets = mutableMapOf<Int, AppTrafficData>()
                val networkTypes = listOf(
                    ConnectivityManager.TYPE_WIFI,
                    ConnectivityManager.TYPE_MOBILE,
                    ConnectivityManager.TYPE_VPN
                )
                
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                for (networkType in networkTypes) {
                    try {
                        val stats = networkStatsManager.querySummary(
                            networkType,
                            null,
                            startTime,
                            System.currentTimeMillis()
                        )
                        val bucket = NetworkStats.Bucket()
                        while (stats.hasNextBucket()) {
                            stats.getNextBucket(bucket)
                            val uid = bucket.uid
                            if (uid > 10000) { // skip system/root
                                val existing = buckets[uid]
                                buckets[uid] = if (existing != null) {
                                    existing.copy(
                                        rxBytes = existing.rxBytes + bucket.rxBytes,
                                        txBytes = existing.txBytes + bucket.txBytes,
                                        totalBytes = existing.totalBytes + bucket.rxBytes + bucket.txBytes
                                    )
                                } else {
                                    val packages = pm.getPackagesForUid(uid)
                                    var packageCache: PackageCache? = null
                                    if (!packages.isNullOrEmpty()) {
                                        try {
                                            val packageInfo = pm.getPackageInfo(packages[0], 0)
                                            val appInfo = packageInfo.applicationInfo
                                            if (appInfo != null) {
                                                packageCache = PackageCache(packageInfo, appInfo, pm)
                                            }
                                        } catch (e: Exception) {}
                                    }
                                    AppTrafficData(packageCache, uid, bucket.rxBytes, bucket.txBytes, bucket.rxBytes + bucket.txBytes)
                                }
                            }
                        }
                        stats.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                buckets.values.filter { it.totalBytes > 0 }.sortedByDescending { it.totalBytes }.take(20)
            }
            isLoadingTraffic = false
        }
    }

    OverrideTopBar {
        TopAppBar(
            title = { Text("Лимит трафика", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Big circular progress
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 24.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = remainingStr,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Осталось",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DataUsage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Использовано", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(String.format("%.1f ГБ", usedGb), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Всего доступно", style = MaterialTheme.typography.titleMedium)
                    Text(limitStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Сброс лимита", style = MaterialTheme.typography.titleMedium)
                    Text(resetStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Buy more button
        if (!isUnlimited && (progress >= 0.9f || remainingGb <= 0.5f)) {
            OutlinedButton(
                onClick = { /* TODO: Open billing */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Icon(Icons.Rounded.AddShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Докупить трафик", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!hasUsageAccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Требуется разрешение",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Для отображения статистики по приложениям, необходимо предоставить разрешение на доступ к истории использования.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text("Предоставить доступ")
                    }
                }
            }
        } else {
            if (isLoadingTraffic) {
                CircularProgressIndicator()
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Трафик приложений (Общий на устройстве)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (appTrafficList.isEmpty()) {
                            Text(
                                "Статистика появится после использования VPN.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            appTrafficList.forEachIndexed { index, appTraffic ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (appTraffic.packageCache?.applicationIcon != null) {
                                        Image(
                                            bitmap = appTraffic.packageCache.applicationIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appTraffic.packageCache?.applicationLabel ?: appTraffic.uid.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = formatBytes(appTraffic.totalBytes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index < appTrafficList.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

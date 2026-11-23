package io.nekohasekai.sfa.compose.screen.dashboard

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.nekohasekai.sfa.compose.shared.PackageCache
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.network.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppTrafficData(
    val packageCache: PackageCache?,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long
)

data class TrafficLimitUiState(
    val trafficUsed: Long = Settings.trafficUsed,
    val trafficLimit: Long = Settings.trafficLimit,
    val quotaResetDate: String? = null,
    val hasUsageAccess: Boolean = false,
    val isLoadingApps: Boolean = false,
    val appTrafficList: List<AppTrafficData> = emptyList()
)

class TrafficLimitViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrafficLimitUiState())
    val uiState: StateFlow<TrafficLimitUiState> = _uiState.asStateFlow()

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        val token = Settings.token
        if (token.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val profile = userRepository.fetchProfile(token)
                    _uiState.update {
                        it.copy(
                            trafficUsed = profile.traffic_used ?: Settings.trafficUsed,
                            trafficLimit = profile.traffic_limit ?: Settings.trafficLimit,
                            quotaResetDate = profile.quota_reset_date
                        )
                    }
                } catch (_: Exception) {
                    // Fall back to local settings
                    _uiState.update {
                        it.copy(
                            trafficUsed = Settings.trafficUsed,
                            trafficLimit = Settings.trafficLimit
                        )
                    }
                }
            }
        }
    }

    fun checkUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        val isAllowed = mode == AppOpsManager.MODE_ALLOWED
        _uiState.update { it.copy(hasUsageAccess = isAllowed) }
        return isAllowed
    }

    fun loadAppTraffic(context: Context) {
        if (!checkUsageAccess(context)) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingApps = true) }
            try {
                val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
                val pm = context.packageManager
                if (networkStatsManager == null) {
                    _uiState.update { it.copy(isLoadingApps = false) }
                    return@launch
                }

                val buckets = mutableMapOf<Int, AppTrafficData>()
                val networkTypes = listOf(
                    ConnectivityManager.TYPE_WIFI,
                    ConnectivityManager.TYPE_MOBILE,
                    ConnectivityManager.TYPE_VPN
                )

                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startTime = calendar.timeInMillis
                val endTime = System.currentTimeMillis()

                for (networkType in networkTypes) {
                    try {
                        val stats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
                        val bucket = NetworkStats.Bucket()
                        while (stats.hasNextBucket()) {
                            stats.getNextBucket(bucket)
                            val uid = bucket.uid
                            if (uid > 10000) { // skip system/root UIDs
                                val existing = buckets[uid]
                                if (existing != null) {
                                    buckets[uid] = existing.copy(
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
                                        } catch (_: Exception) {}
                                    }
                                    buckets[uid] = AppTrafficData(
                                        packageCache = packageCache,
                                        uid = uid,
                                        rxBytes = bucket.rxBytes,
                                        txBytes = bucket.txBytes,
                                        totalBytes = bucket.rxBytes + bucket.txBytes
                                    )
                                }
                            }
                        }
                        stats.close()
                    } catch (_: Exception) {}
                }

                val sortedList = buckets.values
                    .filter { it.totalBytes > 0 }
                    .sortedByDescending { it.totalBytes }
                    .take(25)

                _uiState.update {
                    it.copy(
                        isLoadingApps = false,
                        appTrafficList = sortedList
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingApps = false) }
            }
        }
    }
}

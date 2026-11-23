package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.network.AppNotification
import io.nekohasekai.sfa.network.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val error: String? = null
)

class NotificationsViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val token = Settings.token
            if (token.isBlank()) {
                _uiState.update { it.copy(isLoading = false, notifications = emptyList()) }
                return@launch
            }

            try {
                val remoteNotifications = userRepository.fetchNotifications(token).toMutableList()
                val context = Application.application

                // Check profile subscription expiry to synthesize expiry notices
                try {
                    val profile = userRepository.fetchProfile(token)
                    val endStr = profile.subscription_end
                    if (!endStr.isNullOrBlank()) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val endDate = sdf.parse(endStr)
                        if (endDate != null) {
                            val diffMs = endDate.time - System.currentTimeMillis()
                            val daysLeft = diffMs / (1000 * 60 * 60 * 24)

                            if (diffMs <= 0) {
                                remoteNotifications.add(
                                    0,
                                    AppNotification(
                                        id = -1,
                                        title = context.getString(R.string.notification_expired_title),
                                        message = context.getString(R.string.notification_expired_msg),
                                        type = "subscription",
                                        created_at = null,
                                        is_read = false
                                    )
                                )
                            } else if (daysLeft <= 3) {
                                remoteNotifications.add(
                                    0,
                                    AppNotification(
                                        id = -2,
                                        title = context.getString(R.string.notification_ending_title),
                                        message = context.getString(R.string.notification_ending_msg, daysLeft.toInt()),
                                        type = "subscription",
                                        created_at = null,
                                        is_read = false
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore profile fetch failure during notification check
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notifications = remoteNotifications
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load notifications"
                    )
                }
            }
        }
    }

    fun markAsRead(notificationId: Int) {
        val currentList = _uiState.value.notifications
        val target = currentList.find { it.id == notificationId } ?: return
        if (target.is_read) return

        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map {
                    if (it.id == notificationId) it.copy(is_read = true) else it
                }
            )
        }

        if (notificationId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    userRepository.markNotificationRead(Settings.token, notificationId)
                } catch (_: Exception) {
                    // Ignore transient network failure on mark read
                }
            }
        }
    }

    fun markAllAsRead() {
        val unread = _uiState.value.notifications.filter { !it.is_read }
        if (unread.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                notifications = state.notifications.map { it.copy(is_read = true) }
            )
        }

        val remoteIds = unread.filter { it.id > 0 }.map { it.id }
        if (remoteIds.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val token = Settings.token
                for (id in remoteIds) {
                    try {
                        userRepository.markNotificationRead(token, id)
                    } catch (_: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    fun deleteNotification(notificationId: Int) {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.filter { it.id != notificationId })
        }

        if (notificationId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                val token = Settings.token
                try {
                    userRepository.dismissNotification(token, notificationId)
                } catch (_: Exception) {}
            }
        }
    }

    fun clearAll() {
        _uiState.update { it.copy(notifications = emptyList()) }

        viewModelScope.launch(Dispatchers.IO) {
            val token = Settings.token
            try {
                userRepository.clearAllNotifications(token)
            } catch (_: Exception) {}
        }
    }
}

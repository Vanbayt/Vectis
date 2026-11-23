package io.nekohasekai.sfa.network

import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userApi: UserApi) {

    suspend fun fetchProfile(token: String): UserProfileResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApi.getProfile("Bearer $token")
                
                // Update local settings with fetched traffic data
                Settings.trafficUsed = response.traffic_used ?: 0L
                Settings.trafficLimit = response.traffic_limit ?: 0L
                
                response

            } catch (e: Exception) {
                throw Exception("Failed to fetch user profile: ${e.message}", e)
            }
        }
    }

    suspend fun changePassword(token: String, request: UserPasswordUpdateRequest) {
        withContext(Dispatchers.IO) {
            try {
                userApi.changePassword("Bearer $token", request)
            } catch (e: Exception) {
                throw Exception("Failed to change password: ${e.message}", e)
            }
        }
    }

    suspend fun fetchNotifications(token: String): List<AppNotification> {
        return withContext(Dispatchers.IO) {
            try {
                userApi.getNotifications("Bearer $token")
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun markNotificationRead(token: String, notificationId: Int) {
        withContext(Dispatchers.IO) {
            try {
                userApi.markNotificationRead("Bearer $token", notificationId)
            } catch (_: Exception) {}
        }
    }

    suspend fun dismissNotification(token: String, notificationId: Int) {
        withContext(Dispatchers.IO) {
            try {
                userApi.dismissNotification("Bearer $token", notificationId)
            } catch (_: Exception) {}
        }
    }

    suspend fun clearAllNotifications(token: String) {
        withContext(Dispatchers.IO) {
            try {
                userApi.clearAllNotifications("Bearer $token")
            } catch (_: Exception) {}
        }
    }

    suspend fun regenerateVpnUuid(token: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val resp = userApi.regenerateVpnUuid("Bearer $token")
                resp["vpn_uuid"]
            } catch (e: Exception) {
                throw Exception("Failed to regenerate VPN key: ${e.message}", e)
            }
        }
    }

    suspend fun revokeOtherSessions(token: String) {
        withContext(Dispatchers.IO) {
            try {
                userApi.revokeOtherSessions("Bearer $token")
            } catch (e: Exception) {
                throw Exception("Failed to revoke other sessions: ${e.message}", e)
            }
        }
    }
}

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
                Settings.trafficUsed = response.traffic_used
                Settings.trafficLimit = response.traffic_limit
                
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
}

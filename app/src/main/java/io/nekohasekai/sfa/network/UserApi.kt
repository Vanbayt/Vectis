package io.nekohasekai.sfa.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("/api/v1/user/profile")
    suspend fun getProfile(
        @Header("Authorization") authorization: String
    ): UserProfileResponse

    @PUT("/api/v1/user/password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: UserPasswordUpdateRequest
    )

    @GET("/api/v1/user/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String
    ): List<AppNotification>

    @POST("/api/v1/user/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") authorization: String,
        @Path("id") notificationId: Int
    )

    @DELETE("/api/v1/user/notifications/{id}")
    suspend fun dismissNotification(
        @Header("Authorization") authorization: String,
        @Path("id") notificationId: Int
    )

    @DELETE("/api/v1/user/notifications")
    suspend fun clearAllNotifications(
        @Header("Authorization") authorization: String
    )

    @POST("/api/v1/user/vpn-uuid/regenerate")
    suspend fun regenerateVpnUuid(
        @Header("Authorization") authorization: String
    ): Map<String, String>

    @POST("/api/v1/user/sessions/revoke-others")
    suspend fun revokeOtherSessions(
        @Header("Authorization") authorization: String
    ): Map<String, String>
}

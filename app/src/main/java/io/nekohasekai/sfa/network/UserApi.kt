package io.nekohasekai.sfa.network

import retrofit2.http.GET
import retrofit2.http.Header

interface UserApi {
    @GET("/api/v1/user/profile")
    suspend fun getProfile(
        @Header("Authorization") authorization: String
    ): UserProfileResponse

    @retrofit2.http.PUT("/api/v1/user/password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Body request: UserPasswordUpdateRequest
    )

    @GET("/api/v1/user/notifications")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String
    ): List<AppNotification>

    @retrofit2.http.POST("/api/v1/user/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") notificationId: Int
    )
}


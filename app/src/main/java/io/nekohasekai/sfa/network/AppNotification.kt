package io.nekohasekai.sfa.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AppNotification(
    val id: Int,
    val user_id: Int? = null,
    val title: String,
    val message: String,
    val type: String = "admin", // "admin", "subscription", "system"
    val created_at: String? = null,
    val is_read: Boolean = false
)

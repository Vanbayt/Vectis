package io.nekohasekai.sfa.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class UserProfileResponse(
    val id: Int,
    val username: String,
    val is_active: Boolean,
    val subscription_tier: String,
    val subscription_end: String? = null,
    val traffic_used: Long,
    val traffic_limit: Long
)

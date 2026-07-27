package io.nekohasekai.sfa.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class UserProfileResponse(
    val id: Int,
    val username: String,
    val is_active: Boolean = true,
    val subscription_tier: String = "free",
    val subscription_end: String? = null,
    val traffic_used: Long? = 0L,
    val traffic_limit: Long? = 0L
)

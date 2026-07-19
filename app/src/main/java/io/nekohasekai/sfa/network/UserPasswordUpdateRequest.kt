package io.nekohasekai.sfa.network

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class UserPasswordUpdateRequest(
    val old_password: String,
    val new_password: String
)

package io.nekohasekai.sfa.network

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class VpnConfigResponse(
    @SerialName("encrypted_config")
    val encryptedConfig: String,
    @SerialName("iv")
    val iv: String
)

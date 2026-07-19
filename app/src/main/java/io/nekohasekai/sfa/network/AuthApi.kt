package io.nekohasekai.sfa.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Body
import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

interface AuthApi {
    @FormUrlEncoded
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @POST("/api/v1/auth/register")
    suspend fun register(
        @Body body: Map<String, String>
    )
}

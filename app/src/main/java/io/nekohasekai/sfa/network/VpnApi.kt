package io.nekohasekai.sfa.network

import retrofit2.http.GET
import retrofit2.http.Header

interface VpnApi {

    @GET("/api/v1/vpn/config")
    suspend fun getVpnConfig(
        @Header("Authorization") authHeader: String
    ): VpnConfigResponse
}

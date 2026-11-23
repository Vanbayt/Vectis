package io.nekohasekai.sfa.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface VpnApi {

    @GET("/api/v1/vpn/config")
    suspend fun getVpnConfig(
        @Header("Authorization") authHeader: String,
        @Query("fake_ip") fakeIp: String? = null,
        @Query("mode") mode: String? = null,
        @Query("adblock") adblock: String? = null,
    ): VpnConfigResponse
}

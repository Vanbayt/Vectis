package io.nekohasekai.sfa.network

import android.content.Context
import android.util.Log
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object EndpointManager {

    private const val TAG = "EndpointManager"

    // Primary and fallback endpoints for remote discovery
    private val REMOTE_CONFIG_URLS = listOf(
        "https://gist.githubusercontent.com/Vanbayt/834cb01aa6c9636abba970a53e9a4eb7/raw/endpoint.json"
    )

    private val fastHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var cachedBaseUrl: String = ""

    fun init(context: Context) {
        val saved = Settings.dynamicApiBaseUrl
        cachedBaseUrl = if (saved.isNotBlank() && saved.toHttpUrlOrNull() != null) {
            saved
        } else {
            BuildConfig.API_BASE_URL
        }
        Log.i(TAG, "Initialized with Base URL: $cachedBaseUrl")
        fetchRemoteEndpointAsync()
    }

    fun getBaseUrl(): String {
        if (cachedBaseUrl.isBlank()) {
            cachedBaseUrl = Settings.dynamicApiBaseUrl.ifBlank { BuildConfig.API_BASE_URL }
        }
        return cachedBaseUrl
    }

    fun getTargetHttpUrl() = getBaseUrl().toHttpUrlOrNull()

    fun updateBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (formatted.toHttpUrlOrNull() != null && formatted != cachedBaseUrl) {
            Log.i(TAG, "Updating API Base URL from $cachedBaseUrl to $formatted")
            cachedBaseUrl = formatted
            Settings.dynamicApiBaseUrl = formatted
        }
    }

    fun fetchRemoteEndpointAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            fetchRemoteEndpointSync()
        }
    }

    fun fetchRemoteEndpointSync(): Boolean {
        for (url in REMOTE_CONFIG_URLS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()
                val response = fastHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val newApiUrl = json.optString("api_base_url", "")
                        if (newApiUrl.isNotBlank() && newApiUrl.toHttpUrlOrNull() != null) {
                            updateBaseUrl(newApiUrl)
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch endpoint from $url: ${e.message}")
            }
        }
        return false
    }

    /**
     * OkHttp Interceptor that rewrites request URL host and scheme to current active endpoint.
     */
    fun createInterceptor(): Interceptor = Interceptor { chain ->
        var request = chain.request()
        val targetHttpUrl = getTargetHttpUrl()
        if (targetHttpUrl != null) {
            val originalUrl = request.url
            if (originalUrl.host != targetHttpUrl.host || originalUrl.port != targetHttpUrl.port || originalUrl.scheme != targetHttpUrl.scheme) {
                val newUrl = originalUrl.newBuilder()
                    .scheme(targetHttpUrl.scheme)
                    .host(targetHttpUrl.host)
                    .port(targetHttpUrl.port)
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
        }

        try {
            chain.proceed(request)
        } catch (e: IOException) {
            // Connection failed (e.g. current IP blocked or down) -> trigger background refresh
            fetchRemoteEndpointAsync()
            throw e
        }
    }
}

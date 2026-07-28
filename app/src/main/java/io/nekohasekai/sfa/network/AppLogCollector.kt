package io.nekohasekai.sfa.network

import android.content.Context
import android.util.Log
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.database.ProfileManager
import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AppLogCollector maintains an in-memory ring buffer of diagnostic logs and uploads them to backend API.
 */
object AppLogCollector {
    private const val TAG = "VectisHealth"
    private const val MAX_LOG_ENTRIES = 500

    private val inMemoryLogs = java.util.ArrayDeque<String>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun appendLog(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "$timestamp [$tag] $message"
        if (inMemoryLogs.size >= MAX_LOG_ENTRIES) {
            inMemoryLogs.removeFirst()
        }
        inMemoryLogs.addLast(entry)
        Log.i(tag, message)
    }

    suspend fun collectLogs(): String = withContext(Dispatchers.IO) {
        val logBuilder = StringBuilder()

        // 1. Ensure pre-connect pings are probed if empty
        if (PreConnectPingManager.pingResults.value.isEmpty()) {
            val endpointsToProbe = mutableListOf<PreConnectPingManager.ServerEndpoint>()
            try {
                val selectedId = Settings.selectedProfile
                val profile = runBlocking { ProfileManager.get(selectedId) }
                if (profile != null && profile.typed.path.isNotEmpty()) {
                    val file = File(profile.typed.path)
                    if (file.exists()) {
                        val jsonStr = file.readText()
                        val jsonObj = JSONObject(jsonStr)
                        val obs = jsonObj.optJSONArray("outbounds")
                        if (obs != null) {
                            for (i in 0 until obs.length()) {
                                val ob = obs.optJSONObject(i) ?: continue
                                val type = ob.optString("type")
                                val tag = ob.optString("tag")
                                val host = ob.optString("server")
                                val port = ob.optInt("server_port", 443)

                                if (type == "vless" || type == "hysteria2" || type == "hysteria") {
                                    if (host.isNotEmpty() && port > 0) {
                                        val isUdp = (type == "hysteria2" || type == "hysteria" || type == "tuic")
                                        endpointsToProbe.add(PreConnectPingManager.ServerEndpoint(tag, host, port, isUdp))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }

            if (endpointsToProbe.isNotEmpty()) {
                val latch = java.util.concurrent.CountDownLatch(1)
                PreConnectPingManager.probeAll(endpointsToProbe) {
                    latch.countDown()
                }
                latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }

        logBuilder.append("=== VECTIS HEALTH SUMMARY ===\n")
        logBuilder.append("Selected Outbound Tag: ").append(Settings.selectedOutboundTag).append("\n")
        logBuilder.append("PreConnect Socket Pings: ").append(PreConnectPingManager.pingResults.value).append("\n")
        logBuilder.append("UDP Prober State: ").append(UdpProber.currentState).append("\n")
        logBuilder.append("=============================\n\n")

        // 2. Add In-Memory Ring Buffer logs
        logBuilder.append("--- IN-MEMORY DIAGNOSTIC LOGS ---\n")
        synchronized(this@AppLogCollector) {
            inMemoryLogs.forEach { entry ->
                logBuilder.append(entry).append("\n")
            }
        }
        logBuilder.append("---------------------------------\n\n")

        // 3. Add Logcat if available
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v time VectisHealth:V PreConnectPing:V ConfigInjector:V libbox:V *:S")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var count = 0
            while (bufferedReader.readLine().also { line = it } != null && count < 500) {
                logBuilder.append(line).append("\n")
                count++
            }
            bufferedReader.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture logcat logs: ${e.message}", e)
        }

        return@withContext logBuilder.toString()
    }

    suspend fun uploadLogs(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val logs = collectLogs()
            val token = Settings.token
            if (token.isEmpty()) {
                return@withContext Result.failure(Exception("Пользователь не авторизован"))
            }

            val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/")
            val url = URL("$baseUrl/api/v1/user/logs")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $token")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonBody = JSONObject().apply {
                put("log_content", logs)
                put("device_info", "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val context = io.nekohasekai.sfa.Application.application
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.i(TAG, "Logs successfully uploaded to backend API")
                Result.success(context.getString(io.nekohasekai.sfa.R.string.logs_sent_success))
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                Log.e(TAG, "Failed to upload logs: $err")
                Result.failure(Exception(context.getString(io.nekohasekai.sfa.R.string.server_error_format, responseCode, err)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during log upload: ${e.message}", e)
            Result.failure(e)
        }
    }
}

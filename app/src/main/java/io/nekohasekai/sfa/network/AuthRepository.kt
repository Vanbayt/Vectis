package io.nekohasekai.sfa.network

import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

import io.nekohasekai.sfa.R

class AuthRepository(private val authApi: AuthApi) {

    suspend fun login(username: String, pass: String) {
        val context = io.nekohasekai.sfa.Application.application
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.login(username, pass)
                Settings.token = response.access_token
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    throw Exception(context.getString(R.string.login_error_invalid_credentials))
                } else if (e.code() in 500..504) {
                    throw Exception(context.getString(R.string.login_error_server_unavailable))
                } else {
                    throw Exception(context.getString(R.string.login_error_auth, e.message()))
                }
            } catch (e: Exception) {
                throw Exception(context.getString(R.string.login_error_connection), e)
            }
        }
    }

    suspend fun register(username: String, pass: String, deviceId: String? = null) {
        val context = io.nekohasekai.sfa.Application.application
        withContext(Dispatchers.IO) {
            try {
                val body = mutableMapOf("username" to username, "password" to pass)
                if (!deviceId.isNullOrBlank()) {
                    body["device_id"] = deviceId
                }
                authApi.register(body)
                // After successful registration, log in to get the token
                val response = authApi.login(username, pass)
                Settings.token = response.access_token
            } catch (e: HttpException) {
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                if (errorBody != null && errorBody.contains("DEVICE_FREE_TIER_ALREADY_USED")) {
                    throw Exception(context.getString(R.string.login_error_device_used))
                } else if (e.code() == 400) {
                    throw Exception(context.getString(R.string.login_error_user_exists))
                } else if (e.code() in 500..504) {
                    throw Exception(context.getString(R.string.login_error_server_unavailable))
                } else {
                    throw Exception(context.getString(R.string.login_error_auth, e.message()))
                }
            } catch (e: Exception) {
                throw Exception(e.message ?: context.getString(R.string.login_error_connection), e)
            }
        }
    }

}

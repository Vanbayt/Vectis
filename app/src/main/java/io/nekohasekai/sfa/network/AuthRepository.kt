package io.nekohasekai.sfa.network

import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository(private val authApi: AuthApi) {

    suspend fun login(username: String, pass: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.login(username, pass)
                Settings.token = response.access_token
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    throw Exception("Неверный логин или пароль")
                } else if (e.code() in 500..504) {
                    throw Exception("Сервер недоступен")
                } else {
                    throw Exception("Ошибка авторизации: ${e.message()}")
                }
            } catch (e: Exception) {
                throw Exception("Ошибка подключения к серверу", e)
            }
        }
    }

    suspend fun register(username: String, pass: String, deviceId: String? = null) {
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
                    throw Exception("С этого устройства уже был получен бесплатный лимит 5 ГБ.")
                } else if (e.code() == 400) {
                    throw Exception("Пользователь с таким именем уже существует или некорректные данные")
                } else if (e.code() in 500..504) {
                    throw Exception("Сервер недоступен")
                } else {
                    throw Exception("Ошибка регистрации: ${e.message()}")
                }
            } catch (e: Exception) {
                throw Exception(e.message ?: "Ошибка подключения к серверу", e)
            }
        }
    }

}

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

    suspend fun register(username: String, pass: String) {
        withContext(Dispatchers.IO) {
            try {
                authApi.register(mapOf("username" to username, "password" to pass))
                // After successful registration, log in to get the token
                val response = authApi.login(username, pass)
                Settings.token = response.access_token
            } catch (e: HttpException) {
                if (e.code() == 400) {
                    throw Exception("Пользователь уже существует")
                } else if (e.code() in 500..504) {
                    throw Exception("Сервер недоступен")
                } else {
                    throw Exception("Ошибка регистрации: ${e.message()}")
                }
            } catch (e: Exception) {
                throw Exception("Ошибка подключения к серверу", e)
            }
        }
    }
}

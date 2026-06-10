package io.nekohasekai.sfa.network

import io.nekohasekai.sfa.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VpnRepository(private val vpnApi: VpnApi) {

    /**
     * Получает зашифрованный конфиг с сервера и возвращает его в расшифрованном виде.
     * @param token JWT токен
     * @return расшифрованная JSON-строка конфигурации
     */
    suspend fun fetchAndDecryptConfig(token: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Выполняем GET-запрос к API
                val response = vpnApi.getVpnConfig("Bearer $token")
                
                // Расшифровываем конфигурацию
                CryptoUtils.decryptConfig(response.encryptedConfig, response.iv)
            } catch (e: Exception) {
                // Обрабатываем ошибки (сети, парсинга или расшифровки)
                throw Exception("Failed to fetch or decrypt VPN config: ${e.message}", e)
            }
        }
    }
}

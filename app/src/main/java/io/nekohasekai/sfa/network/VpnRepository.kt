package io.nekohasekai.sfa.network

import io.nekohasekai.sfa.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VpnRepository(private val vpnApi: VpnApi) {

    /**
     * Получает зашифрованный конфиг с сервера и возвращает его в расшифрованном виде.
     * @param token JWT токен
     * @return расшифрованный массив байт конфигурации
     */
    suspend fun fetchAndDecryptConfig(token: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                // Выполняем GET-запрос к API
                val response = vpnApi.getVpnConfig("Bearer $token")
                
                // Расшифровываем конфигурацию
                val configBytes = CryptoUtils.decryptConfig(response.encryptedConfig, response.iv)
                val configString = String(configBytes, Charsets.UTF_8)
                configBytes.fill(0)
                
                // Парсим JSON и исправляем устаревшие поля tun
                val json = org.json.JSONObject(configString)
                val inbounds = json.optJSONArray("inbounds")
                if (inbounds != null) {
                    for (i in 0 until inbounds.length()) {
                        val inbound = inbounds.optJSONObject(i)
                        if (inbound != null && inbound.optString("type") == "tun") {
                            
                            val addressList = org.json.JSONArray()
                            var modified = false
                            
                            // Проверяем inet4_address
                            if (inbound.has("inet4_address")) {
                                val inet4 = inbound.get("inet4_address")
                                if (inet4 is org.json.JSONArray) {
                                    for (j in 0 until inet4.length()) addressList.put(inet4.getString(j))
                                } else if (inet4 is String) {
                                    addressList.put(inet4)
                                }
                                inbound.remove("inet4_address")
                                modified = true
                            }
                            
                            // ПОЛНОСТЬЮ удаляем IPv6 (inet6_address), так как он может вызывать 
                            // IllegalArgumentException (invalid argument) на некоторых Android устройствах
                            if (inbound.has("inet6_address")) {
                                inbound.remove("inet6_address")
                                modified = true
                            }

                            // Если address уже есть, оставляем только IPv4
                            if (inbound.has("address")) {
                                val existing = inbound.getJSONArray("address")
                                for (j in 0 until existing.length()) {
                                    val s = existing.getString(j)
                                    if (!s.contains(":")) { // Пропускаем IPv6
                                        addressList.put(s)
                                    }
                                }
                                modified = true
                            }
                            
                            if (modified && addressList.length() > 0) {
                                inbound.put("address", addressList)
                            }

                            // Удаляем interface_name так как он может вызывать invalid argument на Android
                            if (inbound.has("interface_name")) {
                                inbound.remove("interface_name")
                            }
                            
                            // Добавляем mtu если его нет
                            if (!inbound.has("mtu")) {
                                inbound.put("mtu", 9000)
                            }

                            // Добавляем dns_address чтобы Android отправлял DNS-запросы в TUN
                            if (!inbound.has("dns_address")) {
                                inbound.put("dns_address", "8.8.8.8")
                            }
                        }
                    }
                }
                
                json.toString().toByteArray(Charsets.UTF_8)
            } catch (e: java.net.SocketTimeoutException) {
                throw Exception("TCP-таймаут: Приложение не может достучаться до API сервера. Проверьте: 1) Не включен ли 'Always-on VPN' (Блокировать соединения без VPN). 2) Не запрещен ли доступ к Wi-Fi/данным для этого приложения в настройках Android. Оригинальная ошибка: ${e.message}", e)
            } catch (e: java.net.ConnectException) {
                throw Exception("TCP-соединение сброшено: Доступ заблокирован фаерволом ОС или роутером. Оригинальная ошибка: ${e.message}", e)
            } catch (e: Exception) {
                // Обрабатываем ошибки (сети, парсинга или расшифровки)
                throw Exception("Failed to fetch or decrypt VPN config: ${e.message}", e)
            }
        }
    }
}

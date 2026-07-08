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
                            
                            if (inbound.has("inet4_address")) {
                                val addr = inbound.optString("inet4_address")
                                inbound.remove("inet4_address")
                                val addressArray = org.json.JSONArray()
                                if (addr.isNotEmpty()) {
                                    addressArray.put(addr)
                                }
                                inbound.put("address", addressArray)
                            }

                            if (inbound.has("inet6_address")) {
                                inbound.remove("inet6_address")
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
                // Извлекаем Location и Protocol для отображения в Dashboard
                val outbounds = json.optJSONArray("outbounds")
                var parsedProtocol = "—"
                var parsedLocation = "—"
                if (outbounds != null) {
                    for (i in 0 until outbounds.length()) {
                        val outbound = outbounds.optJSONObject(i)
                        if (outbound != null) {
                            val type = outbound.optString("type")
                            val tag = outbound.optString("tag")
                            // Ищем первый реальный прокси (игнорируем служебные)
                            if (type != "direct" && type != "block" && type != "dns" && type != "urltest" && type != "selector") {
                                parsedProtocol = type.uppercase()
                                // Извлекаем город из тега вида "proxy-frankfurt-0"
                                if (tag.startsWith("proxy-")) {
                                    val parts = tag.split("-")
                                    if (parts.size >= 2) {
                                        parsedLocation = parts[1].replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
                io.nekohasekai.sfa.database.Settings.lastProtocol = parsedProtocol
                io.nekohasekai.sfa.database.Settings.lastLocation = parsedLocation
                
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

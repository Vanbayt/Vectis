package io.nekohasekai.sfa.utils

import android.util.Base64
import io.nekohasekai.sfa.BuildConfig
import java.nio.charset.StandardCharsets
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128

    /**
     * Расшифровывает зашифрованный конфиг.
     * @param cipherTextBase64 зашифрованные данные (Base64)
     * @param ivBase64 вектор инициализации (Base64)
     * @return расшифрованная JSON-строка
     */
    fun decryptConfig(cipherTextBase64: String, ivBase64: String): String {
        try {
            val cipherText = Base64.decode(cipherTextBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            // Получаем ключ из BuildConfig (предполагается, что он 32 байта для AES-256)
            val secretKeyBytes = BuildConfig.AES_SECRET_KEY.toByteArray(StandardCharsets.UTF_8)
            val secretKeySpec = SecretKeySpec(secretKeyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec)

            val decryptedBytes = cipher.doFinal(cipherText)
            return String(decryptedBytes, StandardCharsets.UTF_8)

        } catch (e: AEADBadTagException) {
            throw SecurityException("Failed to decrypt config: Invalid tag or key.", e)
        } catch (e: Exception) {
            throw RuntimeException("Failed to decrypt config", e)
        }
    }
}

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
     * @return расшифрованный массив байт
     */
    fun decryptConfig(cipherTextBase64: String, ivBase64: String): ByteArray {
        var secretKeyBytes: ByteArray? = null
        var cipherText: ByteArray? = null
        var iv: ByteArray? = null
        try {
            cipherText = Base64.decode(cipherTextBase64, Base64.DEFAULT)
            iv = Base64.decode(ivBase64, Base64.DEFAULT)

            secretKeyBytes = NativeLib.getAesKey()
            val secretKeySpec = SecretKeySpec(secretKeyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec)

            return cipher.doFinal(cipherText)
        } catch (e: AEADBadTagException) {
            throw SecurityException("Failed to decrypt config: Invalid tag or key.", e)
        } catch (e: Exception) {
            throw RuntimeException("Failed to decrypt config", e)
        } finally {
            secretKeyBytes?.fill(0)
            cipherText?.fill(0)
            iv?.fill(0)
        }
    }
}

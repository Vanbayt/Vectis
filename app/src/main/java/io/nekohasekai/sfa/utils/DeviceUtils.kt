package io.nekohasekai.sfa.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceUtils {

    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            val rawStr = "VectisSalt_$androidId"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawStr.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "fallback_device_id"
        }
    }
}

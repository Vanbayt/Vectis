package io.nekohasekai.sfa.utils

object NativeLib {
    init {
        System.loadLibrary("vectis-sec")
    }

    external fun getAesKey(): ByteArray
}

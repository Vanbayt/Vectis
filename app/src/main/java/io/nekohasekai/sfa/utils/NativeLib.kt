package io.nekohasekai.sfa.utils

object NativeLib {
    init {
        try {
            System.loadLibrary("vectis-sec")
        } catch (e: UnsatisfiedLinkError) {
            try {
                System.load(io.nekohasekai.sfa.Application.application.applicationInfo.nativeLibraryDir + "/libvectis-sec.so")
            } catch (_: Throwable) {
            }
        }
    }

    external fun getAesKey(): ByteArray

    external fun createTunDevice(ifname: String): Int
}


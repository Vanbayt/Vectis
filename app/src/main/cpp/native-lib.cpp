#include <jni.h>
#include <string>
#include <cstring>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_io_nekohasekai_sfa_utils_NativeLib_getAesKey(JNIEnv* env, jobject /* this */) {
    // "DUMMY_SECRET_KEY_FOR_AES_GCM_32!" obfuscated with XOR 0x42
    const int len = 32;
    unsigned char obf[len] = {
        'D'^0x42, 'U'^0x42, 'M'^0x42, 'M'^0x42, 'Y'^0x42, '_'^0x42, 'S'^0x42, 'E'^0x42,
        'C'^0x42, 'R'^0x42, 'E'^0x42, 'T'^0x42, '_'^0x42, 'K'^0x42, 'E'^0x42, 'Y'^0x42,
        '_'^0x42, 'F'^0x42, 'O'^0x42, 'R'^0x42, '_'^0x42, 'A'^0x42, 'E'^0x42, 'S'^0x42,
        '_'^0x42, 'G'^0x42, 'C'^0x42, 'M'^0x42, '_'^0x42, '3'^0x42, '2'^0x42, '!'^0x42
    };
    
    jbyte key[len];
    for (int i = 0; i < len; i++) {
        key[i] = (jbyte)(obf[i] ^ 0x42);
    }
    
    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, key);
    
    // Clean up local buffer
    memset(key, 0, len);
    
    return result;
}

#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <linux/if.h>
#include <linux/if_tun.h>

extern "C" JNIEXPORT jint JNICALL
Java_io_nekohasekai_sfa_utils_NativeLib_createTunDevice(JNIEnv* env, jobject /* this */, jstring ifname) {
    const char* name = env->GetStringUTFChars(ifname, nullptr);
    int fd = open("/dev/net/tun", O_RDWR | O_NONBLOCK);
    if (fd < 0) {
        fd = open("/dev/tun", O_RDWR | O_NONBLOCK);
    }
    if (fd < 0) {
        env->ReleaseStringUTFChars(ifname, name);
        return -1;
    }
    struct ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    ifr.ifr_flags = IFF_TUN | IFF_NO_PI;
    strncpy(ifr.ifr_name, name, IFNAMSIZ - 1);
    env->ReleaseStringUTFChars(ifname, name);
    
    if (ioctl(fd, TUNSETIFF, (void*)&ifr) < 0) {
        close(fd);
        return -1;
    }
    return fd;
}


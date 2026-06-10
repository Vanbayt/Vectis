package io.nekohasekai.sfa

import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import io.nekohasekai.sfa.di.appModule

class VectisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализируем Koin
        startKoin {
            androidContext(this@VectisApp)
            modules(appModule)
        }
    }
}

package io.nekohasekai.sfa.di

import io.nekohasekai.sfa.compose.screen.dashboard.DashboardViewModel
import io.nekohasekai.sfa.network.VpnApi
import io.nekohasekai.sfa.network.VpnRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val appModule = module {
    single {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.vectis.app") // Это заглушка URL для API, замените на реальный
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(VpnApi::class.java)
    }

    single {
        VpnRepository(get())
    }

    viewModel {
        DashboardViewModel(get())
    }
}

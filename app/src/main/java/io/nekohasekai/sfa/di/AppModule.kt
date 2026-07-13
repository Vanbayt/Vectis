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

import io.nekohasekai.sfa.network.AuthApi
import io.nekohasekai.sfa.network.AuthRepository
import io.nekohasekai.sfa.compose.screen.login.LoginViewModel
import io.nekohasekai.sfa.BuildConfig
import kotlinx.coroutines.launch

val appModule = module {
    single {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code == 401) {
                    io.nekohasekai.sfa.database.Settings.clearSession()
                    kotlinx.coroutines.GlobalScope.launch {
                        io.nekohasekai.sfa.compose.base.GlobalEventBus.emit(io.nekohasekai.sfa.compose.base.UiEvent.ErrorMessage("Сессия истекла. Пожалуйста, авторизуйтесь заново."))
                        io.nekohasekai.sfa.compose.base.GlobalEventBus.emit(io.nekohasekai.sfa.compose.base.UiEvent.Logout)
                    }
                }
                response
            }
            .build()
            
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single {
        get<Retrofit>().create(VpnApi::class.java)
    }

    single {
        get<Retrofit>().create(AuthApi::class.java)
    }

    single {
        VpnRepository(get())
    }

    single {
        AuthRepository(get())
    }

    viewModel {
        DashboardViewModel(get())
    }

    viewModel {
        LoginViewModel(get())
    }
}

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
import io.nekohasekai.sfa.network.UserApi
import io.nekohasekai.sfa.network.UserRepository
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

        val deviceIdInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val deviceId = try {
                io.nekohasekai.sfa.utils.DeviceUtils.getDeviceId(io.nekohasekai.sfa.Application.application)
            } catch (_: Exception) { "" }

            val request = if (deviceId.isNotEmpty()) {
                original.newBuilder().header("X-Device-ID", deviceId).build()
            } else {
                original
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(deviceIdInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code == 401) {
                    io.nekohasekai.sfa.database.Settings.clearSession()
                    kotlinx.coroutines.GlobalScope.launch {
                        val sessionExpiredMsg = io.nekohasekai.sfa.Application.application.getString(io.nekohasekai.sfa.R.string.session_expired)
                        io.nekohasekai.sfa.compose.base.GlobalEventBus.emit(io.nekohasekai.sfa.compose.base.UiEvent.ErrorMessage(sessionExpiredMsg))
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
        get<Retrofit>().create(UserApi::class.java)
    }

    single {
        VpnRepository(get())
    }

    single {
        AuthRepository(get())
    }

    single {
        UserRepository(get())
    }

    viewModel {
        DashboardViewModel(get(), get())
    }

    viewModel {
        LoginViewModel(get())
    }
}

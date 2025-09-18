package com.ntg.lmd.di

import com.ntg.lmd.BuildConfig
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.authentication.domain.repository.AuthRepository
import com.ntg.lmd.authentication.domain.usecase.LoginUseCase
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.repository.DeliveriesLogRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.DeliveriesLogRepository
import com.ntg.lmd.mainscreen.domain.usecase.GetDeliveriesLogFromApiUseCase
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.network.authheader.AuthInterceptor
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import com.ntg.lmd.order.data.remote.OrdersHistoryApi
import com.ntg.lmd.order.data.remote.repository.OrdersRepositoryImpl
import com.ntg.lmd.order.domain.model.repository.OrdersRepository
import com.ntg.lmd.order.domain.model.usecase.GetOrdersUseCase
import com.ntg.lmd.order.ui.viewmodel.OrderHistoryViewModel
import com.ntg.lmd.settings.data.SettingsPreferenceDataSource
import com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel
import com.ntg.lmd.utils.LogoutManager
import com.ntg.lmd.utils.SecureUserStore
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val ordersHistoryModule =
    module {
        single<OrdersRepository> { OrdersRepositoryImpl(get()) }
        factory { GetOrdersUseCase(get()) }
        viewModel { OrderHistoryViewModel(get()) }
    }
val deliveriesLogModule =
    module {
        single<DeliveriesLogRepository> { DeliveriesLogRepositoryImpl(get()) }
        factory { GetDeliveriesLogFromApiUseCase(get()) }
        viewModel { DeliveriesLogViewModel(get()) }
    }

val authModule =
    module {
        // SecureUserStore
        single { SecureUserStore(androidContext()) }

        single {
            AuthInterceptor(
                store = get(),
                supabaseKey = BuildConfig.SUPABASE_KEY,
            )
        }

        single {
            OkHttpClient
                .Builder()
                .addInterceptor(get<AuthInterceptor>())
                .build()
        }

        single {
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(get<OkHttpClient>())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        // AuthApi (Retrofit no auth)
        single<AuthApi> { RetrofitProvider.apiNoAuth }

        // Repository
        single<AuthRepository> {
            AuthRepositoryImp(
                loginApi = get(),
                store = get(), // SecureTokenStore
                userStore = get(), // SecureUserStore
            )
        }

        // UseCase
        factory { LoginUseCase(get()) }

        // ViewModel
        viewModel { LoginViewModel(loginUseCase = get()) }
    }

val networkModule =
    module {
        // SecureTokenStore (only here)
        single { SecureTokenStore(androidContext()) }

        // RetrofitProvider init
        single {
            RetrofitProvider.init(androidContext())
            RetrofitProvider
        }

        // AuthApi without token
        single<AuthApi> { RetrofitProvider.apiNoAuth }
        single<OrdersHistoryApi> { RetrofitProvider.ordersHistoryApi }
        single<OrdersApi> { RetrofitProvider.ordersApi }
    }

val socketModule =
    module {
        single {
            SocketIntegration(
                baseWsUrl = BuildConfig.WS_BASE_URL,
                client = RetrofitProvider.okHttpForWs,
                tokenStore = get<SecureTokenStore>(),
            ).apply {
                get<SecureTokenStore>().onTokensChanged = { access, _ ->
                    reconnectIfTokenChanged(access)
                }
            }
        }
    }

val monitorModule =
    module {
        single { NetworkMonitor(androidContext()) }
    }

val settingsModule =
    module {
        single { SettingsPreferenceDataSource(androidContext()) }

        single {
            LogoutManager(
                tokenStore = get<SecureTokenStore>(),
                socket = get<SocketIntegration>(),
            )
        }

        viewModel {
            SettingsViewModel(
                prefs = get(),
                logoutManager = get(),
            )
        }
    }

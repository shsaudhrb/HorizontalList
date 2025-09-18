package com.ntg.lmd.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.authentication.domain.repository.AuthRepository
import com.ntg.lmd.authentication.domain.usecase.LoginUseCase
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.mainscreen.data.datasource.remote.LiveOrdersApiService
import com.ntg.lmd.mainscreen.data.datasource.remote.UpdatetOrdersStatusApi
import com.ntg.lmd.mainscreen.data.repository.LiveOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.LocationRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.LiveOrdersRepository
import com.ntg.lmd.mainscreen.domain.repository.LocationRepository
import com.ntg.lmd.mainscreen.domain.repository.UpdateOrdersStatusRepository
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetDeviceLocationsUseCase
import com.ntg.lmd.mainscreen.domain.usecase.LoadOrdersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.OrdersRealtimeUseCase
import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import com.ntg.lmd.settings.data.SettingsPreferenceDataSource
import com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel
import com.ntg.lmd.utils.LogoutManager
import com.ntg.lmd.utils.SecureUserStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authModule =
    module {
        // SecureUserStore
        single { SecureUserStore(androidContext()) }

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

        single<LiveOrdersApiService> { RetrofitProvider.liveOrderApi }
        single<UpdatetOrdersStatusApi> { RetrofitProvider.updateStatusApi }
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

val generalPoolModule =
    module {

        // repository
        single<LocationRepository> { LocationRepositoryImpl(get()) }
        single<LiveOrdersRepository> { LiveOrdersRepositoryImpl(get(), get<SocketIntegration>()) }

        // Use cases
        factory { LoadOrdersUseCase(get<LiveOrdersRepository>()) }
        factory { OrdersRealtimeUseCase(get<LiveOrdersRepository>()) }
        factory { ComputeDistancesUseCase() }
        factory { GetDeviceLocationsUseCase(get<LocationRepository>()) }

        // view model
        viewModel {
            GeneralPoolViewModel(
                ordersRealtime = get(),
                computeDistances = get(),
                getDeviceLocations = get(),
                loadOrdersUseCase = get(),
            )
        }
    }

val updateOrderStatusModule =
    module {
        single<UpdateOrdersStatusRepository> { UpdateOrdersStatusRepositoryImpl(get()) }
        factory { UpdateOrderStatusUseCase(get<UpdateOrdersStatusRepository>()) }
    }

val locationModule = module {
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }
}

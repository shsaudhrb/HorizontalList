package com.ntg.lmd.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.ntg.lmd.BuildConfig
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
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule =
    module {
        single { RetrofitProvider.tokenStore }
        single { RetrofitProvider.userStore }

        single<LiveOrdersApiService> { RetrofitProvider.liveOrderApi }
        single<UpdatetOrdersStatusApi> { RetrofitProvider.updateStatusApi }

        // OkHttp for WebSocket
        single<OkHttpClient>(qualifier = named("ws")) { RetrofitProvider.okHttpForWs }
    }

val socketModule =
    module {
        single {
            SocketIntegration(
                baseWsUrl = BuildConfig.WS_BASE_URL,
                client = get<OkHttpClient>(named("ws")),
                tokenStore = get(),
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

val locationModule =
    module {
        single { LocationServices.getFusedLocationProviderClient(get<Context>()) }
    }

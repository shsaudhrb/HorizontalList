package com.ntg.lmd.di

import com.ntg.lmd.BuildConfig
import com.ntg.lmd.mainscreen.data.datasource.remote.LiveOrdersApiService
import com.ntg.lmd.mainscreen.data.repository.LiveOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.LiveOrdersRepository
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetDeviceLocationsUseCase
import com.ntg.lmd.mainscreen.domain.usecase.LoadOrdersUseCase
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel
import com.ntg.lmd.network.sockets.SocketIntegration
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // make sure you have this in buildConfigField
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<LiveOrdersApiService> { get<Retrofit>().create(LiveOrdersApiService::class.java) }
}

val socketModule = module {
    single<SocketIntegration> { SocketIntegrationImpl() }
}

val repositoryModule = module {
    single<LiveOrdersRepository> { LiveOrdersRepositoryImpl(get(), get()) }
}

val useCaseModule = module {
    factory { LoadOrdersUseCase(get<LiveOrdersRepository>()) }
    factory { ComputeDistancesUseCase() }
    factory { GetDeviceLocationsUseCase() }
    factory { OrdersRealtimeUseCase() }
}

val viewModelModule = module {
    viewModel {
        GeneralPoolViewModel(
            repo = get<LiveOrdersRepository>(),
            loadOrders = get<LoadOrdersUseCase>(),
            computeDistances = get<ComputeDistancesUseCase>(),
            getDeviceLocations = get<GetDeviceLocationsUseCase>(),
        )
    }
}
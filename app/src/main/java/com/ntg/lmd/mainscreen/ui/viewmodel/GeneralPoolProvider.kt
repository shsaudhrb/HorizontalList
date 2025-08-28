package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.mainscreen.data.datasource.remote.LiveOrdersApiService
import com.ntg.lmd.mainscreen.data.repository.LocationRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.OrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.LocationRepository
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetDeviceLocationsUseCase
import com.ntg.lmd.mainscreen.domain.usecase.LoadOrdersUseCase
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GeneralPoolProvider {
    private var retrofit: Retrofit? = null
    private var liveApi: LiveOrdersApiService? = null
    private var socket: SocketIntegration? = null
    private var ordersRepo: OrdersRepository? = null
    private var locationRepo: LocationRepository? = null

    private fun retrofit(ctx: Context): Retrofit {
        RetrofitProvider.init(ctx.applicationContext)

        val client: OkHttpClient = RetrofitProvider.okHttpForWs

        if (retrofit == null) {
            retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }
        return retrofit!!
    }

    private fun liveOrdersApi(ctx: Context): LiveOrdersApiService {
        if (liveApi == null) {
            liveApi = retrofit(ctx).create(LiveOrdersApiService::class.java)
        }
        return liveApi!!
    }

    private fun socket(ctx: Context): SocketIntegration {
        val client: OkHttpClient = RetrofitProvider.okHttpForWs
        val tokenStore = SecureTokenStore(ctx.applicationContext)

        if (socket == null) {
            socket =
                SocketIntegration(
                    baseWsUrl = BuildConfig.WS_BASE_URL,
                    client = client,
                    tokenStore = tokenStore,
                )
        }
        return socket!!
    }

    fun ordersRepository(ctx: Context): OrdersRepository {
        if (ordersRepo == null) {
            ordersRepo =
                OrdersRepositoryImpl(
                    liveOrdersApi = liveOrdersApi(ctx),
                    socket = socket(ctx),
                )
        }
        return ordersRepo!!
    }

    fun locationRepository(): LocationRepository {
        if (locationRepo == null) locationRepo = LocationRepositoryImpl()
        return locationRepo!!
    }

    fun loadOrdersUseCase(ctx: Context): LoadOrdersUseCase = LoadOrdersUseCase(ordersRepository(ctx))

    fun getDeviceLocationsUseCase(): GetDeviceLocationsUseCase = GetDeviceLocationsUseCase(locationRepository())

    fun computeDistancesUseCase(): ComputeDistancesUseCase = ComputeDistancesUseCase()
}

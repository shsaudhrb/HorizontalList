package com.ntg.lmd

import android.app.Application
import com.ntg.lmd.di.authModule
import com.ntg.lmd.di.generalPoolModule
import com.ntg.lmd.di.locationModule
import com.ntg.lmd.di.monitorModule
import com.ntg.lmd.di.networkModule
import com.ntg.lmd.di.settingsModule
import com.ntg.lmd.di.socketModule
import com.ntg.lmd.di.updateOrderStatusModule
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.RetrofitProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent

class MyApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        RetrofitProvider.init(this)
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(
                listOf(
                    networkModule,
                    authModule,
                    socketModule,
                    monitorModule,
                    settingsModule,
                    generalPoolModule,
                    updateOrderStatusModule,
                    locationModule,
                ),
            )
        }
        val monitor: NetworkMonitor =
            KoinJavaComponent
                .getKoin()
                .get()
        appScope.launch {
            monitor.isOnline.collect { online ->
            }
        }
    }
}

package com.ntg.lmd

import android.app.Application
import android.content.Context
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.di.MyOrderMyPoolModule
import com.ntg.lmd.di.networkModule
import com.ntg.lmd.di.secureUserStoreModule
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.di.authModule
import com.ntg.lmd.di.monitorModule
import com.ntg.lmd.di.networkModule
import com.ntg.lmd.di.settingsModule
import com.ntg.lmd.di.socketModule
import com.ntg.lmd.network.core.RetrofitProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

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
                    networkModule,
                    secureUserStoreModule,
                    MyOrderMyPoolModule,
                ),
            )
        }
        val monitor: com.ntg.lmd.network.connectivity.NetworkMonitor =
            org.koin.java.KoinJavaComponent
                .getKoin()
                .get()
        appScope.launch {
            monitor.isOnline.collect { online ->
            }
        }
    }
}

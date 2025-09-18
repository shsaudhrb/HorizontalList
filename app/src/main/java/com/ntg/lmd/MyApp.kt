package com.ntg.lmd

import android.app.Application
import android.content.Context
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.di.networkModule
import com.ntg.lmd.di.repositoryModule
import com.ntg.lmd.di.socketModule
import com.ntg.lmd.di.useCaseModule
import com.ntg.lmd.di.viewModelModule
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class MyApp : Application() {
    lateinit var socket: SocketIntegration
        private set
    lateinit var authRepo: AuthRepositoryImp
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var appContext: Context
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(
                networkModule,
                socketModule,
                repositoryModule,
                useCaseModule,
                viewModelModule
            )
        }

        networkMonitor = NetworkMonitor(applicationContext)
        appContext = applicationContext
        RetrofitProvider.init(this)

        socket =
            SocketIntegration(
                baseWsUrl = BuildConfig.WS_BASE_URL,
                client = RetrofitProvider.okHttpForWs,
                tokenStore = RetrofitProvider.tokenStore,
            )

        RetrofitProvider.tokenStore.onTokensChanged = { access, _ ->
            socket.reconnectIfTokenChanged(access)
        }

        authRepo =
            AuthRepositoryImp(
                loginApi = RetrofitProvider.apiNoAuth,
                store = RetrofitProvider.tokenStore,
                userStore = RetrofitProvider.userStore,
            )
        appScope.launch {
            networkMonitor.isOnline.collect { online ->
            }
        }
    }
}

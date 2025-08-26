package com.ntg.lmd

import android.app.Application
import android.util.Log
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApp : Application() {
    lateinit var socket: SocketIntegration
        private set
    lateinit var authRepo: AuthRepositoryImp
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkMonitor(applicationContext)

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
                socket = socket,
            )
        appScope.launch {
            networkMonitor.isOnline.collect { online ->
                Log.d("LMD-NET", "App connectivity changed -> online=$online")
            }
        }
    }
}

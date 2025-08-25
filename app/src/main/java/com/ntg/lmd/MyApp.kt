package com.ntg.lmd

import android.app.Application
import com.ntg.lmd.network.api.AuthRepository
import com.ntg.lmd.network.core.Net
import com.ntg.lmd.network.sockets.SocketIntegration

class MyApp : Application() {
    lateinit var socket: SocketIntegration
        private set
    lateinit var authRepo: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()

        Net.init(this)

        socket =
            SocketIntegration(
                baseWsUrl = BuildConfig.WS_BASE_URL,
                client = Net.okHttpForWs,
                tokenStore = Net.tokenStore,
            )

        Net.tokenStore.onTokensChanged = { access, _ ->
            socket.reconnectIfTokenChanged(access)
        }

        authRepo =
            AuthRepository(
                loginApi = Net.apiNoAuth,
                store = Net.tokenStore,
                socket = socket,
            )
    }
}

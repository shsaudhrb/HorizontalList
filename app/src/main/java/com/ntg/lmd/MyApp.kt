package com.ntg.lmd

import android.app.Application
import com.ntg.lmd.network.authheader.AutoReauthOnConnectivity
import com.ntg.lmd.network.authheader.TokenStore
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.network.sockets.SocketIntegration
import okhttp3.OkHttpClient

class MyApp : Application() {
    private var autoReauth: AutoReauthOnConnectivity? = null

    lateinit var socket: SocketIntegration
        private set

    override fun onCreate() {
        super.onCreate()

        // init Retrofit once
        RetrofitProvider.init(this)

        val tokenStore = TokenStore(this)
        val client = (RetrofitProvider.retrofit.callFactory() as OkHttpClient)

        // create socket instance
        socket =
            SocketIntegration(
                baseWsUrl = "wss://example.com/ws",
                client = client,
                tokenStore = tokenStore,
            )

        autoReauth = AutoReauthOnConnectivity(this).also { it.start() }
    }
}

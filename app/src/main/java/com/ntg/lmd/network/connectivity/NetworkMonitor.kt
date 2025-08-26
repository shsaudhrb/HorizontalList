package com.ntg.lmd.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

class NetworkMonitor(
    ctx: Context,
) {
    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isOnline: StateFlow<Boolean> =
        callbackFlow {
            trySend(currentlyOnline())

            val request = NetworkRequest.Builder().build()
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(true).isSuccess
                    }

                    override fun onLost(network: Network) {
                        val online = currentlyOnline()
                        trySend(online).isSuccess
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        caps: NetworkCapabilities,
                    ) {
                        val online = currentlyOnline()
                        trySend(online).isSuccess
                    }
                }

            cm.registerNetworkCallback(request, callback)
            awaitClose { cm.unregisterNetworkCallback(callback) }
        }.stateIn(scope, SharingStarted.Eagerly, false)

    private fun currentlyOnline(): Boolean =
        cm
            .getNetworkCapabilities(cm.activeNetwork)
            ?.let { caps ->
                listOf(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                ).all(caps::hasCapability)
            } ?: false
}

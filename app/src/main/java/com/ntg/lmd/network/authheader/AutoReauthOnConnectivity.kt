package com.ntg.lmd.network.authheader

import android.content.Context
import android.util.Log
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.queue.ServiceLocator
import com.ntg.lmd.network.queue.storage.QueuedRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "LMD-AutoReauth"
private const val BAD_TOKEN = "BAD_TOKEN"
private const val PROTECTED_PATH = "ping"

class AutoReauthOnConnectivity(
    context: Context,
) {
    private val appCtx = context.applicationContext
    private val tokenStore = TokenStore(appCtx)
    private val monitor = NetworkMonitor(appCtx)
    private val repo by lazy { ServiceLocator.requestRepository(appCtx) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var brokeThisOfflineCycle = false

    fun start() {
        scope.launch {
            monitor.isOnline
                .collect { online ->
                    if (online) onBecameOnline() else onBecameOffline()
                }
        }
        Log.d(TAG, "AutoReauthOnConnectivity started")
    }

    private fun onBecameOffline() {
        if (brokeThisOfflineCycle) return
        val refresh = tokenStore.getRefreshToken()
        if (refresh == null) {
            Log.d(TAG, "Offline: no refresh token -> skip breaking access")
            return
        }
        tokenStore.saveTokens(BAD_TOKEN, refresh)
        brokeThisOfflineCycle = true
        Log.d(TAG, "Offline: access token set to BAD_TOKEN")
    }

    private suspend fun onBecameOnline() {
        if (!brokeThisOfflineCycle) {
            Log.d(TAG, "Online: nothing to reauth (token not broken this cycle)")
            return
        }
        // Kick a protected call via the outbox so it uses the same OkHttp client/interceptors
        val url = BuildConfig.BASE_URL + PROTECTED_PATH
        val id = repo.enqueue(QueuedRequest(method = "GET", url = url))
        Log.d(TAG, "Online: enqueued protected GET $url id=$id to trigger refresh")
        brokeThisOfflineCycle = false
    }

    fun stop() {
        scope.cancel()
        Log.d(TAG, "AutoReauthOnConnectivity stopped")
    }
}

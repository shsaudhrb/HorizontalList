package com.ntg.lmd.utils

import android.util.Log
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.queue.storage.AppDatabase
import com.ntg.lmd.network.sockets.SocketIntegration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LogoutManager(
    private val tokenStore: SecureTokenStore,
    private val socket: SocketIntegration? = null,
    private val db: AppDatabase? = null,
) {
    private companion object {
        private const val TAG = "LogoutManager"
    }

    private suspend fun wipePushState() {
        Log.i(TAG, "FCM: disabling auto-init…")
        FirebaseMessaging.getInstance().setAutoInitEnabled(false)

        Log.i(TAG, "FCM: unsubscribing topics (best effort)…")
        runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic("general").await() }
            .onSuccess { Log.i(TAG, "FCM: unsubscribed from general") }
            .onFailure { Log.w(TAG, "FCM: unsubscribe general failed", it) }

        val userId =
            com.ntg.lmd.network.core.RetrofitProvider.userStore
                .getUserId()
        if (!userId.isNullOrBlank()) {
            runCatching { FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$userId").await() }
                .onSuccess { Log.i(TAG, "FCM: unsubscribed from user_$userId") }
                .onFailure { Log.w(TAG, "FCM: unsubscribe user_$userId failed", it) }
        }

        Log.i(TAG, "FCM: deleting token…")
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onSuccess { Log.i(TAG, "FCM: deleteToken OK") }
            .onFailure { Log.w(TAG, "FCM: deleteToken FAILED", it) }

        Log.i(TAG, "FCM: deleting installation…")
        runCatching { FirebaseInstallations.getInstance().delete().await() }
            .onSuccess { Log.i(TAG, "FCM: installation deleted") }
            .onFailure { Log.w(TAG, "FCM: delete installation FAILED", it) }
    }

    suspend fun logout() =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "Logout started")
            wipePushState()

            runCatching { socket?.disconnect() }
                .onSuccess { Log.i(TAG, "Socket disconnected") }
                .onFailure { Log.w(TAG, "Socket disconnect failed", it) }

            runCatching { db?.clearAllTables() }
                .onSuccess { Log.i(TAG, "DB cleared") }
                .onFailure { Log.w(TAG, "DB clear failed", it) }

            tokenStore.clear()
            Log.i(TAG, "Token store cleared")

            Log.i(TAG, "Logout finished")
        }
}

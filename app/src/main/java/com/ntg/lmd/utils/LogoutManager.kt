package com.ntg.lmd.utils

import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.queue.storage.AppDatabase
import com.ntg.lmd.network.sockets.SocketIntegration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogoutManager(
    private val tokenStore: SecureTokenStore,
    private val socket: SocketIntegration? = null,
    private val db: AppDatabase? = null,
) {
    suspend fun logout() =
        withContext(Dispatchers.IO) {
            runCatching { socket?.disconnect() }
            runCatching { db?.clearAllTables() }
            tokenStore.clear()
        }
}

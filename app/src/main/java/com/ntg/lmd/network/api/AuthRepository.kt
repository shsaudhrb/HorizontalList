package com.ntg.lmd.network.api

import com.ntg.lmd.network.api.dto.LoginRequest
import com.ntg.lmd.network.authheader.TokenStoreTest
import com.ntg.lmd.network.sockets.SocketIntegration

class AuthRepository(
    private val loginApi: TestApi,
    private val store: TokenStoreTest,
    private val socket: SocketIntegration,
) {
    suspend fun login(
        email: String,
        password: String,
    ): Result<Unit> =
        runCatching {
            val env = loginApi.login(LoginRequest(email, password))
            check(env.success) { "success=false" }
            val payload = env.data ?: error("data=null")

            store.saveFromPayload(
                access = payload.accessToken,
                refresh = payload.refreshToken,
                expiresAt = payload.expiresAt,
                refreshExpiresAt = payload.refreshExpiresAt,
            )

            socket.connect()
            socket.joinPhoenixChannel("realtime:live-orders", "1")
        }
}

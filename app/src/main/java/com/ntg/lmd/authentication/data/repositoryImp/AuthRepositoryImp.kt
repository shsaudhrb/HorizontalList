package com.ntg.lmd.authentication.data.repositoryImp

import com.ntg.lmd.authentication.data.datasource.model.LoginRequest
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.sockets.SocketIntegration

class AuthRepositoryImp(
    private val loginApi: AuthApi,
    private val store: SecureTokenStore,
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
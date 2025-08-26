package com.ntg.lmd.authentication.data.repositoryImp

import com.ntg.lmd.authentication.data.datasource.model.LoginRequest
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.queue.NetworkError
import com.ntg.lmd.network.queue.NetworkResult
import retrofit2.HttpException

class AuthRepositoryImp(
    private val loginApi: AuthApi,
    private val store: SecureTokenStore,
) {
    suspend fun login(
        email: String,
        password: String,
    ): NetworkResult<Unit> {
        return try {
            val response = loginApi.login(LoginRequest(email, password))

            if (!response.success) {
                NetworkResult.Error(
                    NetworkError.BadRequest(response.error ?: "Login failed")
                )
            } else {
                val payload = response.data
                if (payload == null) {
                    NetworkResult.Error(
                        NetworkError.BadRequest(response.error ?: "Login failed: no data received")
                    )
                } else {
                    store.saveFromPayload(
                        access = payload.accessToken,
                        refresh = payload.refreshToken,
                        expiresAt = payload.expiresAt,
                        refreshExpiresAt = payload.refreshExpiresAt,
                    )
                    NetworkResult.Success(Unit)
                }
            }
        } catch (e: HttpException) {
            NetworkResult.Error(NetworkError.fromException(e))
        }
    }
}

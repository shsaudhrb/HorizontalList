package com.ntg.lmd.authentication.data.repositoryImp

import com.ntg.lmd.authentication.data.datasource.model.LoginRequest
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.queue.NetworkError
import com.ntg.lmd.network.queue.NetworkResult
import com.ntg.lmd.utils.SecureUserStore
import retrofit2.HttpException

class AuthRepositoryImp(
    private val loginApi: AuthApi,
    private val store: SecureTokenStore,
    private val userStore: SecureUserStore,
) {
    @Volatile var lastLoginName: String? = null

    suspend fun login(
        email: String, password: String
    ): NetworkResult<Unit> =
        try {
            val response = loginApi.login(LoginRequest(email, password))
            if (!response.success) {
                NetworkResult.Error(
                    NetworkError.BadRequest(response.error ?: "Login failed"),)
            } else {
                val payload = response.data
                if (payload == null) {
                    NetworkResult.Error(
                        NetworkError.BadRequest(response.error ?: "Login failed: no data received"),)
                } else {
                    store.saveFromPayload(
                        access = payload.accessToken,
                        refresh = payload.refreshToken,
                        expiresAt = payload.expiresAt,
                        refreshExpiresAt = payload.refreshExpiresAt,
                    )
                    val u = payload.user
                    userStore.saveUser(
                        id = u?.id,
                        email = u?.email,
                        fullName = u?.fullName,
                    )
                    lastLoginName = payload.user?.fullName
                    android.util.Log.d("AuthRepo", "Setting lastLoginName = ${payload.user?.fullName}  repo=${this.hashCode()}")
                    NetworkResult.Success(Unit)
                }
            }
        } catch (e: HttpException) {
            NetworkResult.Error(NetworkError.fromException(e))
        }
}

package com.ntg.lmd.authentication.data.repositoryImp

import com.ntg.lmd.authentication.data.datasource.model.LoginData
import com.ntg.lmd.authentication.data.datasource.model.LoginRequest
import com.ntg.lmd.authentication.data.datasource.model.LoginResponse
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
    @Volatile
    var lastLoginName: String? = null

    suspend fun login(
        email: String,
        password: String,
    ): NetworkResult<Unit> =
        try {
            val payload = validate(loginApi.login(LoginRequest(email, password)))
            persistTokensAndUser(payload)
            captureLastLoginName(payload)
            NetworkResult.Success(Unit)
        } catch (e: IllegalStateException) {
            NetworkResult.Error(NetworkError.BadRequest(e.message ?: "Login failed"))
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (e: retrofit2.HttpException) {
            NetworkResult.Error(NetworkError.fromException(e))
        } catch (e: java.io.IOException) {
            NetworkResult.Error(NetworkError.fromException(e))
        }

    // Ensures success flag true and data present; maps to IllegalStateException for compact guards
    private fun validate(response: LoginResponse?): LoginData {
        if (response == null || !response.success) {
            val msg = response?.error?.takeUnless { it.isNullOrBlank() } ?: "Login failed"
            throw IllegalStateException(msg)
        }
        return response.data
            ?: throw IllegalStateException(response.error ?: "Login failed: no data received")
    }

    // Saves tokens and user atomically from payload; throws if any required token field missing
    private fun persistTokensAndUser(payload: LoginData) {
        val access = payload.accessToken ?: throw IllegalStateException("Missing access token")
        val refresh = payload.refreshToken ?: throw IllegalStateException("Missing refresh token")
        val expiresAt = payload.expiresAt ?: throw IllegalStateException("Missing access token expiry")
        val refreshExpiresAt =
            payload.refreshExpiresAt ?: throw IllegalStateException("Missing refresh token expiry")

        store.saveFromPayload(
            access = access,
            refresh = refresh,
            expiresAt = expiresAt,
            refreshExpiresAt = refreshExpiresAt,
        )

        val u = payload.user
        userStore.saveUser(
            id = u?.id,
            email = u?.email,
            fullName = u?.fullName,
        )
    }

    private fun captureLastLoginName(payload: LoginData) {
        lastLoginName = payload.user?.fullName?.takeUnless { it.isNullOrBlank() }
        android.util.Log.d(
            "AuthRepo",
            "lastLoginName=${lastLoginName ?: "<none>"} repo=${this.hashCode()}",
        )
    }
}

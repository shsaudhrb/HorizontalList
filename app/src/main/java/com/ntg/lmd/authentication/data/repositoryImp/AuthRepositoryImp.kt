package com.ntg.lmd.authentication.data.repositoryImp

import com.ntg.lmd.authentication.data.datasource.model.LoginData
import com.ntg.lmd.authentication.data.datasource.model.LoginRequest
import com.ntg.lmd.authentication.data.datasource.model.LoginResponse
import com.ntg.lmd.authentication.data.datasource.model.User
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.authentication.domain.repository.AuthRepository
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.queue.NetworkError
import com.ntg.lmd.network.queue.NetworkResult
import com.ntg.lmd.utils.SecureUserStore

class AuthRepositoryImp(
    private val loginApi: AuthApi,
    private val store: SecureTokenStore,
    private val userStore: SecureUserStore,
) : AuthRepository {
    @Volatile
    override var lastLoginName: String? = null
        private set

    override suspend fun login(
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

    override fun getCurrentUser(): User? = userStore.getUser()

    override fun isAuthenticated(): Boolean = store.getAccessToken()?.isNotBlank() == true

    // Ensures success flag true and data present; maps to IllegalStateException for compact guards
    private fun validate(response: LoginResponse?): LoginData {
        if (response == null || !response.success) {
            val msg = response?.error?.takeUnless { it.isNullOrBlank() } ?: "Login failed"
            throw IllegalStateException(msg)
        }
        return response.data
            ?: throw IllegalStateException(response.error ?: "Login failed: no data received")
    }

    private fun persistTokensAndUser(payload: LoginData) {
        val access = checkNotNull(payload.accessToken) { "Missing access token" }
        val refresh = checkNotNull(payload.refreshToken) { "Missing refresh token" }
        val expiresAt = checkNotNull(payload.expiresAt) { "Missing access token expiry" }
        val refreshExpiresAt = checkNotNull(payload.refreshExpiresAt) { "Missing refresh token expiry" }

        store.saveFromPayload(
            access = access,
            refresh = refresh,
            expiresAt = expiresAt,
            refreshExpiresAt = refreshExpiresAt,
        )

        payload.user?.let { user ->
            userStore.saveUser(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
            )
        }
    }

    private fun captureLastLoginName(payload: LoginData) {
        lastLoginName = payload.user?.fullName?.takeUnless { it.isNullOrBlank() }
        android.util.Log.d(
            "AuthRepo",
            "lastLoginName=${lastLoginName ?: "<none>"} repo=${this.hashCode()}",
        )
    }
}

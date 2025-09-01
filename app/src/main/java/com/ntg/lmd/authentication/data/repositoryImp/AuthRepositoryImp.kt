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
        email: String,
        password: String,
    ): NetworkResult<Unit> {
        return try {
            val response = loginApi.login(LoginRequest(email, password))

            if (response?.success != true) {
                val msg = response?.error?.takeIf { it.isNullOrBlank().not() } ?: "Login failed"
                return NetworkResult.Error(NetworkError.BadRequest(msg))
            }

            // Guard: payload present
            val payload =
                response.data
                    ?: return NetworkResult.Error(
                        NetworkError.BadRequest(
                            response.error ?: "Login failed: no data received",
                        ),
                    )

            // Guard: required fields present
            val access =
                payload.accessToken
                    ?: return NetworkResult.Error(NetworkError.BadRequest("Login failed: missing access token"))
            val refresh =
                payload.refreshToken
                    ?: return NetworkResult.Error(NetworkError.BadRequest("Login failed: missing refresh token"))
            val expiresAt =
                payload.expiresAt
                    ?: return NetworkResult.Error(NetworkError.BadRequest("Login failed: missing access token expiry"))
            val refreshExpiresAt =
                payload.refreshExpiresAt
                    ?: return NetworkResult.Error(NetworkError.BadRequest("Login failed: missing refresh token expiry"))

            // Persist tokens safely
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
            // Null-safe user name capture
            lastLoginName = payload.user?.fullName?.takeIf { it.isNullOrBlank().not() }

            // Optional: debug log without risking NPEs
            android.util.Log.d(
                "AuthRepo",
                "Setting lastLoginName = ${lastLoginName ?: "<none>"} repo=${this.hashCode()}",
            )

            NetworkResult.Success(Unit)
        } catch (ce: kotlin.coroutines.cancellation.CancellationException) {
            throw ce
        } catch (e: retrofit2.HttpException) {
            NetworkResult.Error(NetworkError.fromException(e))
        } catch (e: java.io.IOException) {
            NetworkResult.Error(NetworkError.fromException(e))
        }
    }
}

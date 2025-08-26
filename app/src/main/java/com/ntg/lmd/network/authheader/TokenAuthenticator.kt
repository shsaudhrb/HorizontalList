package com.ntg.lmd.network.authheader

import android.util.Log
import com.ntg.lmd.authentication.data.datasource.model.RefreshTokenData
import com.ntg.lmd.authentication.data.datasource.model.RefreshTokenRequest
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val HTTP_UNAUTHORIZED = 401
private const val MAX_AUTH_RETRIES = 1

class TokenAuthenticator(
    private val store: SecureTokenStore,
    private val refreshApi: AuthApi,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        var result: Request? = null

        val hadAuth = response.request.header("Authorization") != null
        val prior = count(response)
        if (!hadAuth || response.code != HTTP_UNAUTHORIZED || prior > MAX_AUTH_RETRIES) {
            Log.d("LMD-Auth", "Skip refresh (hadAuth=$hadAuth, prior=$prior, code=${response.code})")
            return null
        }

        val refresh = store.getRefreshToken()
        if (refresh != null) {
            val payload: RefreshTokenData =
                runBlocking(Dispatchers.IO) {
                    mutex.withLock {
                        store.getAccessToken()?.let { existing ->
                            return@withLock RefreshTokenData(
                                existing,
                                store.getRefreshToken(),
                                null,
                                null
                            )
                        }
                        val body = refreshApi.refreshToken(RefreshTokenRequest(refresh))
                        check(body.success) { "Refresh success=false" }
                        body.data ?: error("Refresh data=null")
                    }
                }

            store.saveFromPayload(
                access = payload.accessToken,
                refresh = payload.refreshToken,
                expiresAt = payload.expiresAt,
                refreshExpiresAt = payload.refreshExpiresAt,
            )

            val newAccess = payload.accessToken
            if (newAccess != null) {
                result =
                    response.request
                        .newBuilder()
                        .header("Authorization", "Bearer $newAccess")
                        .build()
            }
        }

        return result
    }

    private fun count(resp: Response): Int {
        var r: Response? = resp
        var c = 0
        while (r != null) {
            c++
            r = r.priorResponse
        }
        return c
    }
}

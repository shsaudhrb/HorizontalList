package com.ntg.lmd.network.authheader

import android.os.Looper
import com.ntg.lmd.authentication.data.datasource.model.LoginData
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
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

private const val HTTP_UNAUTHORIZED = 401
private const val MAX_AUTH_RETRIES = 1

class TokenAuthenticator(
    private val store: SecureTokenStore,
    private val refreshApi: AuthApi,
) : Authenticator {
    private val mutex = Mutex()

    private inline fun <T> runBlockingNotMain(crossinline block: suspend () -> T): T {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return runBlocking(Dispatchers.IO) { block() }
        }
        val out = AtomicReference<T>()
        val err = AtomicReference<Throwable?>()
        val latch = CountDownLatch(1)
        Thread {
            try {
                out.set(runBlocking(Dispatchers.IO) { block() })
            } catch (e: IOException) {
                err.set(e)
            } finally {
                latch.countDown()
            }
        }.start()
        latch.await()
        err.get()?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return out.get()
    }

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val hadAuthHeader = response.request.header("Authorization") != null
        if (!hadAuthHeader || response.code != HTTP_UNAUTHORIZED || responseCount(response) > MAX_AUTH_RETRIES) {
            return null
        }

        val failedAccess =
            response.request
                .header("Authorization")
                ?.removePrefix("Bearer")
                ?.trim()

        val refreshToken = store.getRefreshToken() ?: return null

        val payload: LoginData? =
            try {
                runBlockingNotMain {
                    mutex.withLock {
                        val currentAccess = store.getAccessToken()
                        if (!currentAccess.isNullOrBlank() && currentAccess != failedAccess) {
                            return@withLock LoginData(
                                user = null,
                                accessToken = currentAccess,
                                refreshToken = store.getRefreshToken(),
                                expiresAt = store.getAccessExpiryIso(),
                                refreshExpiresAt = store.getRefreshExpiryIso(),
                            )
                        }

                        val res =
                            refreshApi.refreshToken(
                                RefreshTokenRequest(refresh_token = refreshToken),
                            )
                        if (!res.success || res.data == null) return@withLock null
                        res.data
                    }
                }
            } catch (t: Throwable) {
                null
            }

        if (payload?.accessToken.isNullOrBlank()) return null

        store.saveFromPayload(
            access = payload!!.accessToken,
            refresh = payload.refreshToken,
            expiresAt = payload.expiresAt,
            refreshExpiresAt = payload.refreshExpiresAt,
        )

        return response.request
            .newBuilder()
            .header("Authorization", "Bearer ${payload.accessToken}")
            .build()
    }

    private fun responseCount(r: Response): Int {
        var resp: Response? = r
        var count = 0
        while (resp != null) {
            count++
            resp = resp.priorResponse
        }
        return count
    }
}

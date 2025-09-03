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

private const val MAX_RETRY_ATTEMPTS = 1
private const val HTTP_UNAUTHORIZED = 401

class TokenAuthenticator(
    private val store: SecureTokenStore,
    private val refreshApi: AuthApi,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val shouldAttemptAuth = shouldAuthenticate(response)
        val payload = if (shouldAttemptAuth) tryRefreshToken(extractAccessToken(response)) else null

        return payload?.accessToken?.let {
            response.request
                .newBuilder()
                .header("Authorization", "Bearer $it")
                .build()
        }
    }

    private fun shouldAuthenticate(response: Response): Boolean {
        val hadAuthHeader = response.request.header("Authorization") != null
        return hadAuthHeader && response.code == HTTP_UNAUTHORIZED && responseCount(response) <= MAX_RETRY_ATTEMPTS
    }

    private fun extractAccessToken(response: Response): String? =
        response.request
            .header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

    private fun tryRefreshToken(failedAccess: String?): LoginData? {
        return try {
            runBlockingNotMain {
                mutex.withLock {
                    val currentAccess = store.getAccessToken()
                    if (!currentAccess.isNullOrBlank() && currentAccess != failedAccess) {
                        return@withLock cachedPayload(currentAccess)
                    }

                    val rt = store.getRefreshToken() ?: return@withLock null
                    val res = refreshApi.refreshToken(RefreshTokenRequest(refreshToken = rt))
                    val data = if (res.success) res.data else null

                    if (data?.accessToken.isNullOrBlank()) {
                        store.clear()
                        return@withLock null
                    }

                    store.saveFromPayload(
                        access = data.accessToken,
                        refresh = data.refreshToken,
                        expiresAt = data.expiresAt,
                        refreshExpiresAt = data.refreshExpiresAt,
                    )
                    data
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun cachedPayload(access: String): LoginData =
        LoginData(
            user = null,
            accessToken = access,
            refreshToken = store.getRefreshToken(),
            expiresAt = store.getAccessExpiryIso(),
            refreshExpiresAt = store.getRefreshExpiryIso(),
        )

    private fun responseCount(r: Response): Int {
        var resp: Response? = r
        var count = 0
        while (resp != null) {
            count++
            resp = resp.priorResponse
        }
        return count
    }

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
}

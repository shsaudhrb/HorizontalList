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
            try { out.set(runBlocking(Dispatchers.IO) { block() }) }
            catch (e: IOException) { err.set(e) }
            finally { latch.countDown() }
        }.start()
        latch.await()
        err.get()?.let { throw it }
        @Suppress("UNCHECKED_CAST") return out.get()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val hadAuthHeader = response.request.header("Authorization") != null
        if (!hadAuthHeader || response.code != 401 || responseCount(response) > 1) return null

        val failedAccess = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        // Do all token logic under a single lock
        val payload: LoginData? = try {
            runBlockingNotMain {
                mutex.withLock {
                    val currentAccess = store.getAccessToken()
                    // If another request already refreshed while we were waiting, reuse it.
                    if (!currentAccess.isNullOrBlank() && currentAccess != failedAccess) {
                        return@withLock LoginData(
                            user = null,
                            accessToken = currentAccess,
                            refreshToken = store.getRefreshToken(),
                            expiresAt = store.getAccessExpiryIso(),
                            refreshExpiresAt = store.getRefreshExpiryIso(),
                        )
                    }

                    // Otherwise refresh now, using the *latest* refresh token from the store
                    val rt = store.getRefreshToken() ?: return@withLock null
                    val res = refreshApi.refreshToken(RefreshTokenRequest(refresh_token = rt))
                    val data = if (res.success) res.data else null

                    if (data?.accessToken.isNullOrBlank()) {
                        // hard-fail: clear tokens so app can force re-login
                        store.clear()
                        return@withLock null
                    }

                    // Save inside the lock to avoid races
                    store.saveFromPayload(
                        access = data!!.accessToken,
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

        if (payload?.accessToken.isNullOrBlank()) return null

        return response.request.newBuilder()
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

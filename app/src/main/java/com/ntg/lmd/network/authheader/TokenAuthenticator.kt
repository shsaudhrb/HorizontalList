package com.ntg.lmd.network.authheader

import android.os.Looper
import com.ntg.lmd.authentication.data.datasource.model.LoginRefreshToken
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

    // --- helper to avoid calling runBlocking on the main thread ---------------
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
        var result: Request? = null

        val hadAuth = response.request.header("Authorization") != null
        val prior = count(response)
        if (!hadAuth || response.code != HTTP_UNAUTHORIZED || prior > MAX_AUTH_RETRIES) {
            return null
        }

        val refresh = store.getRefreshToken()
        if (refresh != null) {
            val payload: LoginRefreshToken =
                runBlockingNotMain {
                    mutex.withLock {
                        store.getAccessToken()?.let { existing ->
                            return@withLock LoginRefreshToken(
                                accessToken = existing,
                                refreshToken = store.getRefreshToken(),
                                expiresAt = null,
                                refreshExpiresAt = null,
                                user = null
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

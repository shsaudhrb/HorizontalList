package com.ntg.lmd.network.authheader

import android.util.Log
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.network.api.TestApi
import com.ntg.lmd.network.api.dto.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

private const val MAX_AUTH_RETRIES = 2
private const val HEADER_AUTH = "Authorization"
private const val BEARER_PREFIX = "Bearer "
private const val TAG_AUTH = "LMD-Auth"

class TokenAuthenticator(
    private val store: TokenStore,
    private val api: TestApi,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        Log.d(TAG_AUTH, "authenticate code=${response.code} prior=${responseCount(response)}")
        val hadAuth = response.request.header(HEADER_AUTH) != null
        val eligible =
            responseCount(response) < MAX_AUTH_RETRIES &&
                response.code == HTTP_UNAUTHORIZED &&
                hadAuth
        if (!eligible) {
            Log.d(TAG_AUTH, "skip refresh (eligible=$eligible, hadAuth=$hadAuth)")
            return null
        }
        val refresh = store.getRefreshToken()
        val newAccess =
            refresh?.let {
                runCatching { refreshAccessToken(it) }
                    .onFailure {
                        Log.e(TAG_AUTH, "refresh failed: ${it.message}")
                    }.getOrNull()
            }

        return if (newAccess != null) {
            Log.d(TAG_AUTH, "refresh success -> new access saved")
            store.saveTokens(newAccess, refresh)
            response.request
                .newBuilder()
                .header(HEADER_AUTH, BEARER_PREFIX + newAccess)
                .build()
        } else {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var r: Response? = response.priorResponse
        while (r != null) {
            count++
            r = r.priorResponse
        }
        return count
    }

    private fun refreshAccessToken(refresh: String): String {
        if (BuildConfig.DEBUG && BuildConfig.BASE_URL.contains("httpbin.org")) {
            Log.d(TAG_AUTH, "debug stub: returning dummy access token")
            return "DUMMY_ACCESS_${System.currentTimeMillis()}"
        }
        val res = api.refresh(RefreshRequest(refresh)).execute()
        if (!res.isSuccessful) error("Refresh failed ${res.code()}")
        val body = res.body() ?: error("Empty refresh body")
        return body.accessToken
    }
}

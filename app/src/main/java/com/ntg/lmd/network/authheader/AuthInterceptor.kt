package com.ntg.lmd.network.authheader

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

private const val TAG_AUTH_I = "LMD-Auth-I"

class AuthInterceptor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getAccessToken()
        val req =
            chain
                .request()
                .newBuilder()
                .apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                        Log.d(TAG_AUTH_I, "Added Authorization header")
                    } else {
                        Log.d(TAG_AUTH_I, "No token -> no Authorization header")
                    }
                }.build()
        return chain.proceed(req)
    }
}

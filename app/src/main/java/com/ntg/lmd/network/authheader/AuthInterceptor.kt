package com.ntg.lmd.network.authheader

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val store: SecureTokenStore,
    private val supabaseKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        val isAuthEndpoint = path.endsWith("/login") || path.endsWith("/refresh-token")

        val b = req.newBuilder()
        b.header("apikey", supabaseKey)

        if (isAuthEndpoint) {
            b.header("Authorization", "Bearer $supabaseKey")
        } else {
            store.getAccessToken()?.takeIf { it.isNotBlank() }?.let {
                b.header("Authorization", "Bearer $it")
            } ?: run {
                b.header("Authorization", "Bearer $supabaseKey")
            }
        }

        return chain.proceed(b.build())
    }
}

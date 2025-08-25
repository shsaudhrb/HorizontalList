package com.ntg.lmd.network.authheader

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val store: TokenStoreTest,
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
            Log.d("LMD-Auth-I", "Using anon key for $path")
        } else {
            store.getAccessToken()?.takeIf { it.isNotBlank() }?.let {
                b.header("Authorization", "Bearer $it")
                Log.d("LMD-Auth-I", "Added user access token for ${req.url.encodedPath}")
            } ?: run {
                b.header("Authorization", "Bearer $supabaseKey")
            }
        }

        return chain.proceed(b.build())
    }
}

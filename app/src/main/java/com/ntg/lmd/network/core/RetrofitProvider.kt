package com.ntg.lmd.network.core

import android.content.Context
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.network.authheader.AuthInterceptor
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.authheader.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    lateinit var tokenStore: SecureTokenStore
        private set

    fun init(appCtx: Context) {
        tokenStore = SecureTokenStore(appCtx)
    }

    private val noAuthOkHttp by lazy {
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val key = BuildConfig.SUPABASE_KEY
                val b = chain.request().newBuilder()
                b.header("Authorization", "Bearer $key")
                b.header("apikey", key)
                chain.proceed(b.build())
            }.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    val apiNoAuth: AuthApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(noAuthOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    private val authedOkHttp by lazy {
        OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(tokenStore, BuildConfig.SUPABASE_KEY))
            .authenticator(TokenAuthenticator(tokenStore, apiNoAuth)) // يحدث فقط عند 401
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    val api: AuthApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(authedOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    val okHttpForWs: OkHttpClient get() = authedOkHttp

    val ordersApi: OrdersApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL) // functions root
            .client(authedOkHttp) // includes AuthInterceptor + TokenAuthenticator
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OrdersApi::class.java)
    }
}

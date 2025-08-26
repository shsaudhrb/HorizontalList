package com.ntg.lmd.network.queue

import android.content.Context
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.authentication.data.datasource.remote.api.AuthApi
import com.ntg.lmd.network.authheader.AuthInterceptor
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.authheader.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProviderTest {
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L

    lateinit var retrofit: Retrofit
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    fun init(context: Context) {
        val store = SecureTokenStore(context)

        val refreshApi =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi::class.java)

        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthInterceptor(store, BuildConfig.WS_BASE_URL))
                .authenticator(TokenAuthenticator(store, refreshApi))
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val httpLog =
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            builder.addInterceptor(httpLog)
        }

        okHttpClient = builder.build()

        retrofit =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}

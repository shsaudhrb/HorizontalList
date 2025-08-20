package com.ntg.lmd.network.core

import android.content.Context
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.network.api.TestApi
import com.ntg.lmd.network.authheader.AuthInterceptor
import com.ntg.lmd.network.authheader.TokenAuthenticator
import com.ntg.lmd.network.authheader.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_SECONDS = 20L
private const val READ_TIMEOUT_SECONDS = 20L

object RetrofitProvider {
    lateinit var retrofit: Retrofit
        private set
    lateinit var okHttpClient: OkHttpClient
        private set

    fun init(context: Context) {
        val store = TokenStore(context)
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthInterceptor(store))
                .authenticator(
                    TokenAuthenticator(
                        store,
                        Retrofit
                            .Builder()
                            .baseUrl(BuildConfig.BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(TestApi::class.java),
                    ),
                ).connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

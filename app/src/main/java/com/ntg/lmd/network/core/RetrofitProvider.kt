package com.ntg.lmd.network.core

import android.content.Context
import com.ntg.lmd.BuildConfig
import com.ntg.lmd.network.api.TestApi
import com.ntg.lmd.network.authheader.AuthInterceptor
import com.ntg.lmd.network.authheader.TokenAuthenticator
import com.ntg.lmd.network.authheader.TokenStoreTest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.getValue
import kotlin.jvm.java

object RetrofitProvider {
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L

    lateinit var retrofit: Retrofit
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    fun init(context: Context) {
        val store = TokenStoreTest(context)

        val refreshApi =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TestApi::class.java)

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

object Net {
    lateinit var tokenStore: TokenStoreTest
        private set

    fun init(appCtx: Context) {
        tokenStore = TokenStoreTest(appCtx)
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

    val apiNoAuth: TestApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(noAuthOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TestApi::class.java)
    }

    private val authedOkHttp by lazy {
        OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(tokenStore, BuildConfig.SUPABASE_KEY))
            .authenticator(TokenAuthenticator(tokenStore, apiNoAuth)) // يحدث فقط عند 401
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    val api: TestApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(authedOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TestApi::class.java)
    }

    val okHttpForWs: OkHttpClient get() = authedOkHttp
}

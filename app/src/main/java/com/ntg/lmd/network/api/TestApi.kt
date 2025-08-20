package com.ntg.lmd.network.api

import com.ntg.lmd.network.api.dto.RefreshRequest
import com.ntg.lmd.network.api.dto.RefreshResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TestApi {
    @POST("auth/refresh")
    fun refresh(
        @Body body: RefreshRequest,
    ): Call<RefreshResponse>

    @GET("status/401")
    suspend fun force401(): ResponseBody
}

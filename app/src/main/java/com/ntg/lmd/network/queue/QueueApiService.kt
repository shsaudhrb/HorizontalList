package com.ntg.lmd.network.queue

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Url

interface QueueApiService {
    @GET
    suspend fun get(
        @Url url: String,
    ): Response<Unit>

    @POST
    suspend fun post(
        @Url url: String,
        @Body body: RequestBody? = null,
    ): Response<Unit>

    @PUT
    suspend fun put(
        @Url url: String,
        @Body body: RequestBody? = null,
    ): Response<Unit>

    @DELETE
    suspend fun delete(
        @Url url: String,
    ): Response<Unit>

    @PATCH
    suspend fun patch(
        @Url url: String,
        @Body body: RequestBody? = null,
    ): Response<Unit>
}

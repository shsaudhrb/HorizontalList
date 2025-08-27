package com.ntg.lmd.mainscreen.data.datasource.remote

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface LiveOrdersApiService {
    @GET("live-orders")
    suspend fun getLiveOrdersPage(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("search") search: String? = null,
    ): PagedOrdersResponse
}

data class PagedOrdersResponse(
    val success: Boolean,
    val data: JsonElement?,
    val message: String? = null,
    // Page-based pagination
    val page: Int? = null,
    val totalPages: Int? = null,
    val nextPage: Int? = null,
    val hasMore: Boolean? = null,
)

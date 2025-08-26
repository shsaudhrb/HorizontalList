package com.ntg.lmd.mainscreen.data.datasource.remote

import com.ntg.lmd.mainscreen.data.model.LiveOrdersResponse
import retrofit2.http.GET

interface LiveOrdersApiService {
    @GET("live-orders")
    suspend fun getLiveOrders(): LiveOrdersResponse
}

data class OrdersApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)
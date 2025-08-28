package com.ntg.lmd.order.data.remote

import com.ntg.lmd.order.data.remote.dto.OrdersListResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OrdersApi {
    @GET("orders-list")
    suspend fun getOrders(
        @Header("Authorization") token: String,
        @Query("status_id") statusIds: String = "3,7,8",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): OrdersListResponse
}
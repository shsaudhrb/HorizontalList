package com.ntg.lmd.mainscreen.data.datasource.remote

import com.ntg.lmd.mainscreen.data.model.OrdersEnvelope
import retrofit2.http.GET
import retrofit2.http.Query

interface OrdersApi {
    @GET("orders-list")
    suspend fun getOrders(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): OrdersEnvelope
}

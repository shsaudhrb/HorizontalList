package com.ntg.lmd.order.data.remote

import com.ntg.lmd.order.data.remote.dto.OrdersListResponse
import com.ntg.lmd.order.domain.model.OrderStatusCode
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OrdersHistoryApi {
    @GET("orders-list")
    suspend fun getOrders(
        @Header("Authorization") token: String,
        @Query("status_id") statusIds: String =
            OrderStatusCode.fromList(
                listOf(
                    OrderStatusCode.CANCELLED,
                    OrderStatusCode.FAILED,
                    OrderStatusCode.DONE,
                ),
            ),
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): OrdersListResponse
}

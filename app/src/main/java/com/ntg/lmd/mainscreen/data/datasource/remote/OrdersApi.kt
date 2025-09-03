package com.ntg.lmd.mainscreen.data.datasource.remote

import com.ntg.lmd.mainscreen.data.model.OrdersEnvelope
import retrofit2.http.GET
import retrofit2.http.Query
@Suppress("LongParameterList")
interface OrdersApi {
    @GET("orders-list")
    suspend fun getOrders(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("status_ids[]") statusIds: List<Int>? = null,
        @Query("search") search: String? = null,
        @Query("assigned_agent_id") assignedAgentId: String? = null,
        @Query("user_orders_only") userOrdersOnly: Boolean? = null,
    ): OrdersEnvelope
}

package com.ntg.lmd.order.data.remote.repository

import com.ntg.lmd.order.data.remote.OrdersApi
import com.ntg.lmd.order.data.remote.dto.toUi
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.repository.OrdersRepository

class OrdersRepositoryImpl(
    private val api: OrdersApi
) : OrdersRepository {

    override suspend fun getOrders(token: String, page: Int, limit: Int): List<OrderHistoryUi> {
        val response = api.getOrders("Bearer $token", page = page, limit = limit)
        return if (response.success) {
            response.data.orders.map { it.toUi() }
        } else emptyList()
    }
}
package com.ntg.lmd.order.domain.model.repository

import com.ntg.lmd.order.domain.model.OrderHistoryUi

interface OrdersRepository {
    suspend fun getOrders(token: String, page: Int, limit: Int): List<OrderHistoryUi>
}
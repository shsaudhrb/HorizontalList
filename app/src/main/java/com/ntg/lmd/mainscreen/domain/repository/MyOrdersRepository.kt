package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

interface MyOrdersRepository {
    suspend fun getOrders(
        page: Int,
        limit: Int,
    ): List<OrderInfo>
}

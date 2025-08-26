package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

interface OrdersRepository {
    suspend fun getOrders(): List<OrderInfo>
}

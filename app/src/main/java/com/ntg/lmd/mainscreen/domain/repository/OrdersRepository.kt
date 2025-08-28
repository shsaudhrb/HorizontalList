package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.data.model.Order

interface OrdersRepository {
    suspend fun getAllLiveOrders(pageSize: Int = 50): Result<List<Order>>

    fun connectToOrders(channelName: String = "orders")

    fun disconnectFromOrders()

    fun retryConnection()

    fun updateOrderStatus(
        orderId: String,
        status: String,
    )
}

package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.data.model.LiveOrdersResponse
import kotlinx.coroutines.flow.Flow

interface OrdersRepository {
    suspend fun getLiveOrders(): Flow<Result<LiveOrdersResponse>>
    fun connectToOrders(channelName: String = "orders")
    fun disconnectFromOrders()
    fun retryConnection()
    fun updateOrderStatus(orderId: String, status: String)
}
package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.repository.LiveOrdersRepository
import kotlinx.coroutines.flow.StateFlow

class OrdersRealtimeUseCase(
    private val repo: LiveOrdersRepository,
) {
    fun connect(channelName: String = "orders") = repo.connectToOrders(channelName)

    fun disconnect() = repo.disconnectFromOrders()

    fun retry() = repo.retryConnection()

    fun orders(): StateFlow<List<Order>> = repo.orders()
}
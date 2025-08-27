package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository

class LoadOrdersUseCase(
    private val repo: OrdersRepository,
) {
    suspend operator fun invoke(pageSize: Int = 50): Result<List<Order>> = repo.getAllLiveOrders(pageSize)
}

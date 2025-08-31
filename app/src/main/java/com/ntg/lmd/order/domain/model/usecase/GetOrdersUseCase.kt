package com.ntg.lmd.order.domain.model.usecase

import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.repository.OrdersRepository

class GetOrdersUseCase(
    private val repository: OrdersRepository,
) {
    suspend operator fun invoke(
        token: String,
        page: Int,
        limit: Int,
    ): List<OrderHistoryUi> = repository.getOrders(token, page, limit)
}

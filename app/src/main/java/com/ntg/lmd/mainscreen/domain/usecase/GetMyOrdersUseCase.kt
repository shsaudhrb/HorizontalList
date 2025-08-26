package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository

class GetMyOrdersUseCase(
    private val repo: OrdersRepository,
) {
    suspend operator fun invoke(): List<OrderInfo> = repo.getOrders()
}

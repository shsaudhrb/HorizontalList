package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository

class GetMyOrdersUseCase(
    private val repo: MyOrdersRepository,
) {
    suspend operator fun invoke(
        page: Int,
        limit: Int,
    ): List<OrderInfo> = repo.getOrders(page, limit)
}

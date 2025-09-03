package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.domain.model.OrdersPage
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository

class GetMyOrdersUseCase(
    private val repo: MyOrdersRepository,
) {
    suspend operator fun invoke(
        page: Int,
        limit: Int,
        bypassCache: Boolean = false,
        assignedAgentId: String? = null,
        userOrdersOnly: Boolean? = null,
    ): OrdersPage = repo.getOrders(page, limit,bypassCache,assignedAgentId,userOrdersOnly)
}

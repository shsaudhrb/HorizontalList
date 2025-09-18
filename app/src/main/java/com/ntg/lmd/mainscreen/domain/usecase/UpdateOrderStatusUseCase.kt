package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepositoryImpl
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

class UpdateOrderStatusUseCase(
    private val repo: UpdateOrdersStatusRepositoryImpl,
) {
    suspend operator fun invoke(
        orderId: String,
        statusId: Int,
        assignedAgentId: String? = null,
    ): OrderInfo = repo.updateOrderStatus(orderId, statusId, assignedAgentId)
}

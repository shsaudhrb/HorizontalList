package com.ntg.lmd.mainscreen.domain.usecase


import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepository
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

class UpdateOrderStatusUseCase(private val repo: UpdateOrdersStatusRepository) {
    suspend operator fun invoke(
        orderId: String,
        statusId: Int,
        assignedAgentId: String? = null
    ): OrderInfo = repo.updateOrderStatus(orderId, statusId, assignedAgentId)
}

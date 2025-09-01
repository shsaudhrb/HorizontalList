package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.UpdatetOrdersStatusApi
import com.ntg.lmd.mainscreen.data.model.UpdateOrderStatusRequest
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.apiIdToOrderStatus
import com.ntg.lmd.mainscreen.domain.repository.UpdateOrdersStatus

class UpdateOrdersStatusRepository(
    private val api: UpdatetOrdersStatusApi,
) : UpdateOrdersStatus {
    override suspend fun updateOrderStatus(
        orderId: String, statusId: Int,
        assignedAgentId: String?
    ): OrderInfo {
        val env =
            api.updateOrderStatus(
                UpdateOrderStatusRequest(
                    orderId = orderId,
                    statusId = statusId,
                    assignedAgentId = assignedAgentId,
                ),
            )
        if (!env.success) error(env.message ?: "Failed to update order status")
        val d = env.data
        val updated =
            OrderInfo(
                id = d?.orderId ?: orderId,
                name = d?.customerName ?: "",
                orderNumber = d?.orderNumber ?: "",
                timeAgo = "now",
                itemsCount = 0,
                distanceKm = 0.0,
                lat = 0.0,
                lng = 0.0,
                status = apiIdToOrderStatus(d?.statusId),
                price = "---",
                customerPhone = null,
                details = d?.address,
                customerId = null,
                assignedAgentId = d?.assignedAgentId ?: assignedAgentId,
            )

        return updated
    }
}

package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.datasource.remote.UpdatetOrdersStatusApi
import com.ntg.lmd.mainscreen.data.model.UpdateOrderStatusRequest
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.apiIdToOrderStatus
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository
import com.ntg.lmd.mainscreen.domain.repository.UpdateOrdersStatus

class UpdateOrdersStatusRepository (private val api: UpdatetOrdersStatusApi): UpdateOrdersStatus {
    override suspend fun updateOrderStatus(
        orderId: String,
        statusId: Int,
        assignedAgentId: String?
    ): OrderInfo {
        val env = api.updateOrderStatus(
            UpdateOrderStatusRequest(
                orderId = orderId,
                statusId = statusId,
                assignedAgentId = assignedAgentId
            )
        )
        if (!env.success) error(env.message ?: "Failed to update order status")

        // Use response if it contains richer info; otherwise patch locally with minimal fields.
        val d = env.data
        // Build a minimal domain from the response:
        val updated = OrderInfo(
            id = d?.orderId ?: orderId,
            name = d?.customerName ?: "",
            orderNumber = d?.orderNumber ?: "",
            // keep timeAgo/itemsCount/price if you need from cache; for simplicity default:
            timeAgo = "now",
            itemsCount = 0,
            distanceKm = 0.0,
            lat = 0.0,
            lng = 0.0,
            status = apiIdToOrderStatus(d?.statusId),
            price = "---",
            customerPhone = null,
            details = d?.address
        )


        return updated
    }

}


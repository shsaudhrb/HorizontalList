package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.UpdatetOrdersStatusApi
import com.ntg.lmd.mainscreen.data.model.UpdateOrderStatusRequest
import com.ntg.lmd.mainscreen.data.model.UpdatedOrderData
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus.Companion.fromId
import com.ntg.lmd.mainscreen.domain.repository.UpdateOrdersStatus

class UpdateOrdersStatusRepository(
    private val api: UpdatetOrdersStatusApi,
) : UpdateOrdersStatus {
    override suspend fun updateOrderStatus(
        orderId: String,
        statusId: Int,
        assignedAgentId: String?,
    ): OrderInfo {
        val req = buildRequest(orderId, statusId, assignedAgentId)
        android.util.Log.d("OrderAction", "POST body = $req") // will print the data class
        val env = api.updateOrderStatus(req)
        require(env.success) { env.message ?: "Failed to update order status" }
        return mapToOrderInfo(env.data, orderId, assignedAgentId)
    }

    private fun buildRequest(
        orderId: String,
        statusId: Int,
        assignedAgentId: String?,
    ) = UpdateOrderStatusRequest(
        orderId = orderId,
        statusId = statusId,
        assignedAgentId = assignedAgentId,
    )

    private fun mapToOrderInfo(
        d: UpdatedOrderData?,
        fallbackOrderId: String,
        fallbackAssigned: String?,
    ): OrderInfo =
        OrderInfo(
            id = d?.orderId ?: fallbackOrderId,
            name = d?.customerName.orEmpty(),
            orderNumber = d?.orderNumber.orEmpty(),
            timeAgo = "now",
            itemsCount = 0,
            distanceKm = 0.0,
            lat = 0.0,
            lng = 0.0,
            status = fromId(d?.statusId),
            price = "---",
            customerPhone = null,
            details = d?.address,
            customerId = null,
            assignedAgentId = d?.assignedAgentId ?: fallbackAssigned,
        )
}

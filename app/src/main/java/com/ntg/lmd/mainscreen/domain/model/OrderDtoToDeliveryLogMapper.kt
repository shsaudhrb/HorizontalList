package com.ntg.lmd.mainscreen.domain.model

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.usecase.DeliveryStatusIds

private fun mapState(statusId: Int?): DeliveryState =
    when (statusId) {
        DeliveryStatusIds.DELIVERED -> DeliveryState.DELIVERED
        DeliveryStatusIds.FAILED -> DeliveryState.FAILED
        DeliveryStatusIds.CANCELLED -> DeliveryState.CANCELLED
        else -> DeliveryState.OTHER
    }

fun OrderDto.toDeliveryLog(): DeliveryLog =
    DeliveryLog(
        orderId = "#$orderNumber",
        orderDate = orderDate.orEmpty(),
        deliveryTime = deliveryTime.orEmpty(),
        state = mapState(statusId),
    )

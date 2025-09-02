package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus.Companion.fromId

fun OrderDto.toDomain(): OrderInfo =
    OrderInfo(
        id = orderId,
        name = customerName.orEmpty(),
        orderNumber = orderNumber,
        timeAgo = "now",
        itemsCount = 0,
        distanceKm = distanceKm ?: Double.NaN,
        lat = coordinates?.latitude ?: Double.NaN,
        lng = coordinates?.longitude ?: Double.NaN,
        status = fromId(statusId),
        price = "---",
        customerPhone = null,
        customerId = customerId,
        assignedAgentId = assignedAgentId,
        details = null,
    )

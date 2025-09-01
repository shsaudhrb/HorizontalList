package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.apiIdToOrderStatus

// / Renad
fun OrderDto.toDomain(): OrderInfo =
    OrderInfo(
        id = orderId,
        name = customerName.orEmpty(),
        orderNumber = orderNumber,
        timeAgo = "now", // you can replace this with proper relative time later
        itemsCount = 0, // not provided in DTO
        distanceKm = distanceKm ?: Double.NaN,
        lat = coordinates?.latitude ?: Double.NaN,
        lng = coordinates?.longitude ?: Double.NaN,
        status = apiIdToOrderStatus(statusId),
        price = "---",
        customerPhone = null,
        customerId = customerId,
        assignedAgentId = assignedAgentId,
        details = null,
    )

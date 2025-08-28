package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus

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
        status =
            when (orderstatuses?.statusName?.lowercase()) {
                "added" -> OrderStatus.ADDED
                "confirmed" -> OrderStatus.CONFIRMED
                "picked" -> OrderStatus.PICKUP
                "reassigned" -> OrderStatus.REASSIGNED
                "canceled" -> OrderStatus.CANCELED
                "delivered" -> OrderStatus.DELIVERY_DONE
                "deliver failed" -> OrderStatus.DELIVERY_FAILED
                "start delivery" -> OrderStatus.START_DELIVERY
                else -> OrderStatus.ADDED
            },
        price = "---",
        customerPhone = null,
        details = null,
    )

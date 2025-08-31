package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus

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
        status =
            OrderStatus.fromId(statusId)
                ?: when (orderstatuses?.statusName?.trim()?.lowercase()) {
                    "added" -> OrderStatus.ADDED
                    "confirmed" -> OrderStatus.CONFIRMED
                    "picked", "pickup" -> OrderStatus.PICKUP
                    "reassigned" -> OrderStatus.REASSIGNED
                    "canceled", "cancelled" -> OrderStatus.CANCELED
                    "delivered", "delivery done" -> OrderStatus.DELIVERY_DONE
                    "deliver failed", "delivery failed" -> OrderStatus.DELIVERY_FAILED
                    "start delivery" -> OrderStatus.START_DELIVERY
                    else -> OrderStatus.ADDED
                },
        price = "---",
        customerPhone = null,
        details = null,
    )

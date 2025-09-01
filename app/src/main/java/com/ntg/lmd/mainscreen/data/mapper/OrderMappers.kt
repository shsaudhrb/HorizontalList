package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.utils.AppDefaults

fun OrderDto.toDomain(): OrderInfo =
    OrderInfo(
        id = orderId,
        name = customerName.orEmpty(),
        orderNumber = orderNumber,
        timeAgo = "now",
        itemsCount = 0, // not provided in API
        distanceKm = distanceKm ?: AppDefaults.DEFAULT_DISTANCE_KM,
        lat = coordinates?.latitude ?: AppDefaults.DEFAULT_LAT,
        lng = coordinates?.longitude ?: AppDefaults.DEFAULT_LNG,
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

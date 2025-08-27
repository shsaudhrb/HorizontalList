package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus

fun OrderDto.toDomain(): OrderInfo =
    OrderInfo(
        name = customerName.orEmpty(),
        orderNumber = orderNumber,
        timeAgo = "now", // you can replace this with proper relative time later
        itemsCount = 0, // not provided in DTO
        distanceKm = distanceKm ?: Double.NaN,
        lat = coordinates?.latitude ?: Double.NaN,
        lng = coordinates?.longitude ?: Double.NaN,
        status =
            when (orderstatuses?.statusName?.lowercase()) {
                "added" -> OrderStatus.NEW
                "confirmed" -> OrderStatus.CONFIRMED
                "picked" -> OrderStatus.PICKED
                "on_route" -> OrderStatus.ON_ROUTE
                "delivered" -> OrderStatus.DELIVERED
                else -> OrderStatus.NEW
            },
        price = 0.0,
    )

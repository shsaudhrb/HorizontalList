package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus.Companion.fromId
import com.ntg.lmd.utils.AppDefaults

private const val METERS_PER_KILOMETER = 1_000.0

private fun Double?.toKmOrDefault(default: Double): Double {
    val meters = this ?: return default
    return if (meters.isFinite() && meters >= 0.0) meters / METERS_PER_KILOMETER else default
}

fun OrderDto.toDomain(): OrderInfo =
    OrderInfo(
        id = orderId,
        name = customerName.orEmpty(),
        orderNumber = orderNumber,
        timeAgo = "now",
        itemsCount = 0,
        distanceKm = distanceKm.toKmOrDefault(AppDefaults.DEFAULT_DISTANCE_KM),
        lat = coordinates?.latitude ?: AppDefaults.DEFAULT_LAT,
        lng = coordinates?.longitude ?: AppDefaults.DEFAULT_LNG,
        status = fromId(statusId),
        price = price,
        customerPhone = phone,
        customerId = customerId,
        assignedAgentId = assignedAgentId,
        details = null,
    )

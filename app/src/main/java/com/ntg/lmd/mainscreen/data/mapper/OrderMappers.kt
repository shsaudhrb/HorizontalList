package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus.Companion.fromId
import com.ntg.lmd.utils.AppDefaults

private const val METER_TO_KM = 1000.0
private const val MIN_DISTANCE_METERS = 0.0

private fun Double?.toKmOrDefault(default: Double): Double =
    if (this != null && this.isFinite() && this >= MIN_DISTANCE_METERS) {
        this / METER_TO_KM
    } else {
        default
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
        price = "---",
        customerPhone = null,
        customerId = customerId,
        assignedAgentId = assignedAgentId,
        details = null,
    )

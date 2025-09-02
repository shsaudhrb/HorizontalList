package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderDto
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.apiIdToOrderStatus
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.utils.AppDefaults

//fun OrderDto.toDomain(): OrderInfo =
//    OrderInfo(
//        id = orderId,
//        name = customerName.orEmpty(),
//        orderNumber = orderNumber,
//        timeAgo = "now",
//        itemsCount = 0,
//        distanceKm = distanceKm ?: AppDefaults.DEFAULT_DISTANCE_KM,
//        lat = coordinates?.latitude ?: AppDefaults.DEFAULT_LAT,
//        lng = coordinates?.longitude ?: AppDefaults.DEFAULT_LNG,
//        status = apiIdToOrderStatus(statusId),
//        price = "---",
//        customerPhone = null,
//        customerId = customerId,
//        assignedAgentId = assignedAgentId,
//        details = null,
//    )

private fun Double?.toKmOrDefault(default: Double): Double =
    if (this != null && this.isFinite() && this >= 0.0) this / 1000.0 else default

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

        status = apiIdToOrderStatus(statusId),
        price = "---",
        customerPhone = null,
        customerId = customerId,
        assignedAgentId = assignedAgentId,
        details = null,
    )

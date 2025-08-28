package com.ntg.lmd.order.data.remote.dto

import com.ntg.lmd.order.domain.model.OrderHistoryStatus
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import java.time.Instant

fun OrderHistoryDto.toUi(): OrderHistoryUi {
    return OrderHistoryUi(
        orderId = orderId,
        number = orderNumber,
        customer = customerName,
        createdAtMillis = parseDate(lastUpdated),
        status = when (orderStatus.statusName.lowercase()) {
            "cancelled", "canceled" -> OrderHistoryStatus.CANCELLED
            "delivery failed" -> OrderHistoryStatus.FAILED
            "delivery done", "delivered" -> OrderHistoryStatus.DELIVERED
            else -> OrderHistoryStatus.FAILED
        },
        total = 0.0 ,
        statusColor = orderStatus.colorCode
    )
}

private fun parseDate(date: String): Long {
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        formatter.parse(date)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

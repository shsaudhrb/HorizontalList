package com.ntg.lmd.order.data.remote.dto

import android.util.Log
import com.ntg.lmd.order.domain.model.OrderHistoryStatus
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

fun OrderHistoryDto.toUi(): OrderHistoryUi {
    val statusName = orderStatus.statusName.lowercase()
    return OrderHistoryUi(
        orderId = orderId,
        number = orderNumber,
        customer = customerName,
        createdAtMillis = parseDate(lastUpdated),
        status =
            when (orderStatus.statusName.lowercase()) {
                "cancelled", "canceled" -> OrderHistoryStatus.CANCELLED
                "delivery failed" -> OrderHistoryStatus.FAILED
                "delivery done", "delivered" -> OrderHistoryStatus.DONE
                else -> {
                    Log.w("OrderMapper", "Unknown status from API: ${orderStatus.statusName}")
                    OrderHistoryStatus.UNKNOWN
                }
            },
        total = 0.0,
        statusColor = orderStatus.colorCode,
        isCancelled = statusName == "cancelled" || statusName == "canceled",
        isFailed = statusName == "delivery failed",
        isDelivered = statusName == "delivery done" || statusName == "delivered",
    )
}

// Parse API date string into epoch milliseconds
private fun parseDate(date: String): Long =
    try {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
        formatter.parse(date)?.time ?: 0L
    } catch (e: ParseException) {
        Log.e("OrderMapper", "Invalid date format: $date", e)
        0L
    }

package com.ntg.lmd.mainscreen.ui.mapper

import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.model.RelativeTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// ---- Time constants ----
private val MINUTE_MILLIS: Long = TimeUnit.MINUTES.toMillis(1)
private val HOUR_MILLIS: Long = TimeUnit.HOURS.toMillis(1)
private val DAY_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

private val isoParsers by lazy {
    listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
}

private fun parseIsoToMillis(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    return isoParsers.firstNotNullOfOrNull { fmt ->
        try {
            fmt.parse(s)?.time
        } catch (_: ParseException) {
            null
        }
    }
}

private fun formatRelative(then: Long?): RelativeTime {
    if (then == null) return RelativeTime.Unknown
    val now = System.currentTimeMillis()
    val diff = abs(now - then)

    return when {
        diff < MINUTE_MILLIS -> RelativeTime.JustNow
        diff < HOUR_MILLIS -> RelativeTime.MinutesAgo((diff / MINUTE_MILLIS).toInt())
        diff < DAY_MILLIS -> {
            val h = (diff / HOUR_MILLIS).toInt()
            if (h == 1) RelativeTime.HoursAgo(1) else RelativeTime.HoursAgo(h)
        }

        else -> {
            val d = (diff / DAY_MILLIS).toInt()
            if (d == 1) RelativeTime.DaysAgo(1) else RelativeTime.DaysAgo(d)
        }
    }
}

fun Order.toUi(): OrderInfo {
    val lat = coordinates?.latitude ?: latitude ?: 0.0
    val lng = coordinates?.longitude ?: longitude ?: 0.0

    val whenMillis =
        parseIsoToMillis(lastUpdated)
            ?: parseIsoToMillis(orderDate)
            ?: parseIsoToMillis(deliveryTime)

    val timeAgo = formatRelative(whenMillis)

    return OrderInfo(
        id = orderId ?: id ?: orderNumber ?: "-",
        name = customerName ?: "-",
        orderNumber = orderNumber ?: orderId ?: id ?: "-",
        status = OrderStatus.fromId(statusId),
        assignedAgentId = assignedAgentId,
        timeAgo = timeAgo,
        itemsCount = 0,
        distanceKm = Double.POSITIVE_INFINITY,
        lat = lat,
        lng = lng,
        customerPhone = phone,
        details = address,
    )
}

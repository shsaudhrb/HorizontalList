package com.ntg.lmd.mainscreen.ui.mapper

import android.content.Context
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
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

private fun formatRelative(
    ctx: Context,
    then: Long?,
): String {
    if (then == null) return "-"
    val now = System.currentTimeMillis()
    val diff = abs(now - then)

    return when {
        diff < MINUTE_MILLIS -> ctx.getString(R.string.time_just_now)
        diff < HOUR_MILLIS ->
            ctx.getString(
                R.string.time_minutes_ago,
                (diff / MINUTE_MILLIS).toInt(),
            )

        diff < DAY_MILLIS -> {
            val h = (diff / HOUR_MILLIS).toInt()
            if (h == 1) {
                ctx.getString(R.string.time_one_hour_ago)
            } else {
                ctx.getString(R.string.time_hours_ago, h)
            }
        }

        else -> {
            val d = (diff / DAY_MILLIS).toInt()
            if (d == 1) {
                ctx.getString(R.string.time_one_day_ago)
            } else {
                ctx.getString(R.string.time_days_ago, d)
            }
        }
    }
}

fun Order.toUi(context: Context): OrderInfo {
    val lat = coordinates?.latitude ?: latitude ?: 0.0
    val lng = coordinates?.longitude ?: longitude ?: 0.0

    val whenMillis =
        parseIsoToMillis(lastUpdated)
            ?: parseIsoToMillis(orderDate)
            ?: parseIsoToMillis(deliveryTime)

    val timeAgo = formatRelative(context, whenMillis)

    return OrderInfo(
        name = customerName ?: "-",
        orderNumber = orderNumber ?: orderId ?: id ?: "-",
        timeAgo = timeAgo,
        itemsCount = 0,
        distanceKm = Double.POSITIVE_INFINITY,
        lat = lat,
        lng = lng,
        customerPhone = null,
        details = null
    )
}
//fun OrderDto.toDomain(): Order = Order(
//    id = null,
//    orderId = orderId,
//    orderNumber = orderNumber,
//    customerName = customerName,
//    address = address,
//    statusId = statusId,
//    assignedAgentId = null,
//    partnerId = null,
//    dcId = null,
//    orderDate = orderDate,
//    deliveryTime = deliveryTime,
//    lastUpdated = lastUpdated,
//    coordinates = coordinates,
//)

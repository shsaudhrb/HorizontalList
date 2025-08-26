package com.ntg.lmd.mainscreen.ui.mapper

import android.content.Context
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val isoParsers by lazy {
    listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
}

private fun parseIsoToMillis(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    for (fmt in isoParsers) {
        try {
            return fmt.parse(s)?.time
        } catch (_: ParseException) {
        }
    }
    return null
}

private fun formatRelative(context: Context, then: Long?): String {
    if (then == null) return "-"

    val now = System.currentTimeMillis()
    val diff = abs(now - then)

    val min = 60_000L
    val hr = 60 * min
    val day = 24 * hr

    return when {
        diff < min -> context.getString(R.string.time_just_now)
        diff < hr -> context.getString(R.string.time_minutes_ago, (diff / min).toInt())
        diff < day -> {
            val h = diff / hr
            if (h == 1L) context.getString(R.string.time_one_hour_ago)
            else context.getString(R.string.time_hours_ago, h.toInt())
        }

        else -> {
            val d = diff / day
            if (d == 1L) context.getString(R.string.time_one_day_ago)
            else context.getString(R.string.time_days_ago, d.toInt())
        }
    }
}

fun Order.toUi(context: Context): OrderInfo {
    val lat = coordinates?.latitude ?: 0.0
    val lng = coordinates?.longitude ?: 0.0

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
        lng = lng
    )

}


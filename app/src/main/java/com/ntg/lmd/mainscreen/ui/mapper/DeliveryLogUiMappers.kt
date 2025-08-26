package com.ntg.lmd.mainscreen.ui.mapper

import android.content.Context
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.model.DeliveryLogDomain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Time constants in milliseconds
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

private val dateFormatter by lazy {
    SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
}

// convert domain model into UI model with formatted strings
fun DeliveryLogDomain.toUi(context: Context): DeliveryLog {
    val orderDate = dateFormatter.format(Date(createdAtMillis))
    return DeliveryLog(
        orderDate = orderDate,
        deliveryTime = formatRelativeTime(context, createdAtMillis),
        orderId = "#$number",
        state = state,
    )
}

// format time "20 minutes ago"
private fun formatRelativeTime(
    context: Context,
    thenMillis: Long,
): String {
    val now = System.currentTimeMillis()
    val diff = abs(now - thenMillis)

    return when {
        diff < MINUTE_MS -> context.getString(R.string.time_just_now)
        diff < HOUR_MS -> context.getString(R.string.time_minutes_ago, diff / MINUTE_MS)
        diff < DAY_MS -> {
            val h = diff / HOUR_MS
            if (h == 1L) {
                context.getString(R.string.time_one_hour_ago)
            } else {
                context.getString(R.string.time_hours_ago, h)
            }
        }

        else -> {
            val d = diff / DAY_MS
            if (d == 1L) {
                context.getString(R.string.time_one_day_ago)
            } else {
                context.getString(R.string.time_days_ago, d)
            }
        }
    }
}

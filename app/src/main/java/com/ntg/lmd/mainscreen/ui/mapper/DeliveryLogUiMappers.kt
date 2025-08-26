package com.ntg.lmd.mainscreen.ui.mapper

import android.content.Context
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.model.DeliveryLogDomain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
        state = state
    )
}

// format time "20 minutes ago"
private fun formatRelativeTime(context: Context, thenMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = abs(now - thenMillis)

    val minuteMs = 60_000L
    val hourMs = 60 * minuteMs
    val dayMs = 24 * hourMs

    return when {
        diff < minuteMs -> context.getString(R.string.time_just_now)
        diff < hourMs -> context.getString(R.string.time_minutes_ago, diff / minuteMs)
        diff < dayMs -> {
            val h = diff / hourMs
            if (h == 1L) context.getString(R.string.time_one_hour_ago)
            else context.getString(R.string.time_hours_ago, h)
        }

        else -> {
            val d = diff / dayMs
            if (d == 1L) context.getString(R.string.time_one_day_ago)
            else context.getString(R.string.time_days_ago, d)
        }
    }
}

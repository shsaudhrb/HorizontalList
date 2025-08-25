package com.ntg.lmd.notification.ui.model

import kotlin.math.abs

private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

private const val MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND
private const val MILLIS_PER_HOUR = MINUTES_PER_HOUR * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = HOURS_PER_DAY * MILLIS_PER_HOUR

fun relativeAgeLabel(
    nowMs: Long,
    thenMs: Long,
): String {
    val diff = abs(nowMs - thenMs)

    val minutes = diff / MILLIS_PER_MINUTE
    val hours = diff / MILLIS_PER_HOUR
    val days = diff / MILLIS_PER_DAY

    return when {
        minutes == 0L -> "just now"
        minutes < MINUTES_PER_HOUR -> "$minutes min ago"
        hours < HOURS_PER_DAY -> "$hours hr${if (hours == 1L) "" else "s"} ago"
        else -> "$days day${if (days == 1L) "" else "s"} ago"
    }
}

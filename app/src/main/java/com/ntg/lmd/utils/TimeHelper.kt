package com.ntg.lmd.utils

import android.content.Context
import com.ntg.lmd.R
import java.util.concurrent.TimeUnit

private const val HOURS_IN_DAY = 24L
private const val MINUTES_IN_HOUR = 60L

fun timeHelper(
    context: Context,
    fromMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val d = (nowMillis - fromMillis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(d)
    val hours = TimeUnit.MINUTES.toHours(minutes)
    val days = TimeUnit.HOURS.toDays(hours)

    return when {
        days > 0L -> {
            val hRemainder = (hours % HOURS_IN_DAY).toInt()
            context.getString(R.string.time_d_h_ago, days.toInt(), hRemainder)
        }
        hours > 0L -> {
            val mRemainder = (minutes % MINUTES_IN_HOUR).toInt()
            context.getString(R.string.time_h_m_ago, hours.toInt(), mRemainder)
        }
        minutes > 0L -> {
            context.getString(R.string.time_m_ago, minutes.toInt())
        }
        else -> context.getString(R.string.time_just_now)
    }
}

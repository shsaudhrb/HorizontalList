package com.ntg.lmd.order.ui.components

fun timeHelper(fromMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val d = (nowMillis - fromMillis).coerceAtLeast(0)
    val minutes = d / (60_000)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0     -> "${days}d ${hours % 24}h ago"
        hours > 0    -> "${hours}h ${minutes % 60}m ago"
        minutes > 0  -> "${minutes}m ago"
        else         -> "just now"
    }
}
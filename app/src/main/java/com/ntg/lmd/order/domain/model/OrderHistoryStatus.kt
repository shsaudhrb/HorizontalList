package com.ntg.lmd.order.domain.model

enum class OrderHistoryStatus { CANCELLED, FAILED, DONE, UNKNOWN }

// Filter options for orders history (allowed statuses + sort order by date)
data class OrdersHistoryFilter(
    val allowed: Set<OrderHistoryStatus> =
        setOf(
            OrderHistoryStatus.CANCELLED,
            OrderHistoryStatus.FAILED,
            OrderHistoryStatus.DONE,
            OrderHistoryStatus.UNKNOWN,
        ),
    val ageAscending: Boolean = false,
)

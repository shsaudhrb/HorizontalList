package com.ntg.lmd.order.domain.model

enum class OrderHistoryStatus { DELIVERED, CANCELLED, FAILED, UNKNOWN }

// Filter options for orders history (allowed statuses + sort order by date)
data class OrdersHistoryFilter(
    val allowed: Set<OrderHistoryStatus> =
        setOf(
            OrderHistoryStatus.DELIVERED,
            OrderHistoryStatus.CANCELLED,
            OrderHistoryStatus.FAILED,
            OrderHistoryStatus.UNKNOWN,
        ),
    val ageAscending: Boolean = false,
)

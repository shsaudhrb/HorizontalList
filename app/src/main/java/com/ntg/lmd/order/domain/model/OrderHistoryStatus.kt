package com.ntg.lmd.order.domain.model

enum class OrderHistoryStatus { DELIVERED, CANCELLED, FAILED }

data class OrdersHistoryFilter(
    val allowed: Set<OrderHistoryStatus> =
        setOf(
            OrderHistoryStatus.DELIVERED,
            OrderHistoryStatus.CANCELLED,
            OrderHistoryStatus.FAILED,
        ),
    val ageAscending: Boolean = false,
)

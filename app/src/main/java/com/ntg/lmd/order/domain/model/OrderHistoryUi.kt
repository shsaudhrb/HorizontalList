package com.ntg.lmd.order.domain.model

data class OrderHistoryUi(
    val number: String,
    val customer: String,
    val total: Double,
    val status: OrderHistoryStatus,
    val createdAtMillis: Long,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
)

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
data class OrdersHistoryUiState(
    val orders: List<OrderHistoryUi> = emptyList(),
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val isRefreshing: Boolean = false
)

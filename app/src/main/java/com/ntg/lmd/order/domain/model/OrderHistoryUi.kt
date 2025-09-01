package com.ntg.lmd.order.domain.model

data class OrderHistoryUi(
    val orderId: String,
    val number: String,
    val customer: String,
    val createdAtMillis: Long,
    val status: OrderHistoryStatus,
    val total: Double,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val statusColor: String = "",
    val isCancelled: Boolean = false,
    val isFailed: Boolean = false,
    val isDelivered: Boolean = false,
)

data class OrdersHistoryUiState(
    val orders: List<OrderHistoryUi> = emptyList(),
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val isRefreshing: Boolean = false,
)

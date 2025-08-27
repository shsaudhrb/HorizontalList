package com.ntg.lmd.mainscreen.ui.screens.orders.model

data class OrderUI( // single order
    val id: Long = 0,
    val orderNumber: String,
    val status: String,
    val customerName: String,
    val customerPhone: String?,
    val totalPrice: Double,
    val details: String?,
    val distanceMeters: Double?,
)

data class MyOrdersUiState(
    val orders: List<OrderUI> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false, // for paging
    val isRefreshing: Boolean = false, // for refresh
    val isGpsAvailable: Boolean = true, // If false, distance is unavailable;
    val query: String = "", // current query to search
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
)

enum class OrderStatus { ADDED, CONFIRMED, DISPATCHED, DELIVERING, DELIVERED, FAILED, CANCELED }

val OrderUI.statusEnum: OrderStatus
    get() =
        when (status.lowercase()) {
            "added" -> OrderStatus.ADDED
            "confirmed" -> OrderStatus.CONFIRMED
            "dispatched" -> OrderStatus.DISPATCHED
            "delivering" -> OrderStatus.DELIVERING
            "delivered" -> OrderStatus.DELIVERED
            "failed" -> OrderStatus.FAILED
            "canceled" -> OrderStatus.CANCELED
            else -> OrderStatus.ADDED
        }

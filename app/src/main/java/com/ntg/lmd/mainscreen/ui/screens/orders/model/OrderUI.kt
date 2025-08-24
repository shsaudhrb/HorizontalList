package com.ntg.lmd.mainscreen.ui.screens.orders.model

data class OrderUI( // single order
    val id: Long,
    val orderNumber: String,
    val status: String,
    val customerName: String,
    val totalPrice: Double,
    val details: String?,
    val distanceMeters: Double?,
)

data class Order(
    val orderNumber: String,
    val customerName: String,
    val details: String,
    val price: String,
    val distanceMeters: Double?,
)

data class MyOrdersUiState(
    val orders: List<OrderUI> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isGpsAvailable: Boolean = true,
    val query: String = "",
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
)

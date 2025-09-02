package com.ntg.lmd.mainscreen.ui.screens.orders.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class MyOrdersUiState(
    val orders: List<OrderInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false, // for paging
    val isRefreshing: Boolean = false, // for refresh
    val isGpsAvailable: Boolean = true, // If false, distance is unavailable;
    val query: String = "", // current query to search
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
    val page: Int? = 0,
    val endReached: Boolean = false,
)

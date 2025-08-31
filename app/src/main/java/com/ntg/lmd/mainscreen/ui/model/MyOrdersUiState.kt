package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class MyOrdersUiState(
    val orders: List<OrderInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isGpsAvailable: Boolean = true,
    val query: String = "",
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
)

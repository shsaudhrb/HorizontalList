package com.ntg.lmd.mainscreen.ui.viewmodel

import android.location.Location
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import kotlinx.coroutines.flow.MutableStateFlow

class OrdersStore(
    val state: MutableStateFlow<MyOrdersUiState>,
    val currentUserId: MutableStateFlow<String?>,
    val deviceLocation: MutableStateFlow<Location?>,
    val allOrders: MutableList<OrderInfo> = mutableListOf(),
) {
    var page: Int = 1
    var endReached: Boolean = false
}
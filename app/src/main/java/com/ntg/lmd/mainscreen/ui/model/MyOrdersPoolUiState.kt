package com.ntg.lmd.mainscreen.ui.model

import android.util.Log
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class MyOrdersPoolUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val orders: List<OrderInfo> = emptyList(),
    val selectedOrderNumber: String? = null,
    override val distanceThresholdKm: Double = Double.MAX_VALUE,
) : MapUiState {
    override val mapOrders: List<OrderInfo>
        get() {
            Log.d("MAP", "Orders on map: ${orders.size}")
            return orders
        }
    override val selected: OrderInfo?
        get() = orders.find { it.orderNumber == selectedOrderNumber }
}

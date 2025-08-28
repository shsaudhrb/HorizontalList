package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

interface MapUiState {
    val mapOrders: List<OrderInfo>
    val selected: OrderInfo?
    val distanceThresholdKm: Double
    val hasLocationPerm: Boolean
}

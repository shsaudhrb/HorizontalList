package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class GeneralPoolUiState(
    val mapOrders: List<OrderInfo> = emptyList(),
    val selected: OrderInfo? = null,
    val distanceThresholdKm: Double = 10.0
)
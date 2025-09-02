package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class BottomCallbacks(
    val onAddClick: (OrderInfo) -> Unit,
    val onOrderClick: (OrderInfo) -> Unit,
    val onCentered: (OrderInfo, Int) -> Unit,
)

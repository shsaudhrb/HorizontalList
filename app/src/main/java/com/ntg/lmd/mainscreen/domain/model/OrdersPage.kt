package com.ntg.lmd.mainscreen.domain.model

data class OrdersPage(
    val items: List<OrderInfo>,
    val rawCount: Int, // server page size BEFORE filtering
)

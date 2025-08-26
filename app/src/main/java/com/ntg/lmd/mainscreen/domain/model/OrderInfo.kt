package com.ntg.lmd.mainscreen.domain.model

data class OrderInfo(
    val name: String = "",
    val orderNumber: String = "",
    val timeAgo: String = "0m ago",
    val itemsCount: Int = 0,
    val distanceKm: Double = 0.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: OrderStatus = OrderStatus.NEW,
    val price: Double = 0.0,
)

enum class OrderStatus { NEW, CONFIRMED, PICKED, ON_ROUTE, DELIVERED }

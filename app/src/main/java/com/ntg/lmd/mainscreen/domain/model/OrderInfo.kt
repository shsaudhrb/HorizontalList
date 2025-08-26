package com.ntg.lmd.mainscreen.domain.model

data class OrderInfo(
    val name: String,
    val orderNumber: String,
    val timeAgo: String,
    val itemsCount: Int,
    val distanceKm: Double,
    val lat: Double,
    val lng: Double,
)

package com.ntg.lmd.mainscreen.domain.model

data class DeliveryLog(
    val orderDate: String,   // dd/MM/yyyy h:mm a
    val deliveryTime: String, // 20 mins ago"
    val orderId: String,     // #1234
    val state: DeliveryState
)

data class DeliveryLogDomain(
    val number: String,
    val createdAtMillis: Long,
    val state: DeliveryState
)
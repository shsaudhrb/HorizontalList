package com.ntg.lmd.order.domain.model

data class OrderUi(
    val number: String,
    val customer: String,
    val total: Double,
    val status: OrderStatus,
    val createdAtMillis: Long,
)

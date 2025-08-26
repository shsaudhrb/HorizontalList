package com.ntg.lmd.mainscreen.data.model

data class OrderRecord(
    val number: String,
    val customer: String,
    val total: Double,
    val status: String,
    val createdAtMillis: Long,
)

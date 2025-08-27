package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class OrderItem(
    @SerializedName("id")
    val id: String?,
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("quantity")
    val quantity: Int?,
    @SerializedName("unit_price")
    val unitPrice: Double?,
    @SerializedName("total_price")
    val totalPrice: Double?,
)

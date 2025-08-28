package com.ntg.lmd.order.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrderHistoryDto(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("order_date") val orderDate: String,
    @SerializedName("last_updated") val lastUpdated: String,
    @SerializedName("orderstatuses") val orderStatus: OrderStatusDto
)

data class OrderStatusDto(
    @SerializedName("status_name") val statusName: String,
    @SerializedName("color_code") val colorCode: String,
    @SerializedName("font_color_code") val fontColorCode: String
)
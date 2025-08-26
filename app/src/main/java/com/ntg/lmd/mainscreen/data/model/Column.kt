package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class Column(
    @SerializedName("name")
    val name: String?,
    @SerializedName("type")
    val type: String?
)

data class OrderStatusUpdate(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("status")
    val status: String
)

data class Coordinates(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)
package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class Column(
    @SerializedName("name")
    val name: String?,
    @SerializedName("type")
    val type: String?,
)

data class OrderStatusUpdate(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("status")
    val status: String,
)

data class Coordinates(
    @SerializedName(value = "latitude", alternate = ["lat"]) val latitude: Double?,
    @SerializedName(value = "longitude", alternate = ["lng", "lon"]) val longitude: Double?,
)

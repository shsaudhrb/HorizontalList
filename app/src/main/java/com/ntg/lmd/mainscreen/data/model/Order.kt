package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class Order(
    @SerializedName("id") val id: String? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("status_id") val statusId: Int? = null,
    @SerializedName("assigned_agent_id") val assignedAgentId: String? = null,
    @SerializedName("partner_id") val partnerId: String? = null,
    @SerializedName("dc_id") val dcId: String? = null,
    @SerializedName(value = "order_date", alternate = ["orderDate"]) val orderDate: String? = null,
    @SerializedName(value = "delivery_time", alternate = ["deliveryTime"]) val deliveryTime: String? = null,
    @SerializedName(value = "last_updated", alternate = ["updatedAt", "lastUpdated"]) val lastUpdated: String? = null,
    @SerializedName("coordinates") val coordinates: Coordinates,
    @SerializedName(value = "latitude", alternate = ["lat"]) val latitude: Double? = null,
    @SerializedName(value = "longitude", alternate = ["lng", "lon"]) val longitude: Double? = null,
)

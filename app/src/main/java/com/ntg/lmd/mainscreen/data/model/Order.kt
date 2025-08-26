package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

// Map this to your payload; only the fields you use are required
data class Order(
    @SerializedName("id") val id: String? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("customer_id") val customerId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("status_id") val statusId: Int? = null,
    @SerializedName("assigned_agent_id") val assignedAgentId: String? = null,
    @SerializedName("partner_id") val partnerId: String? = null,
    @SerializedName("dc_id") val dcId: String? = null,
    @SerializedName("order_date") val orderDate: String? = null,
    @SerializedName("delivery_time") val deliveryTime: String? = null,
    @SerializedName("sla_met") val slaMet: Boolean? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("coordinates") val coordinates: Coordinates? = null,
    @SerializedName("last_updated") val lastUpdated: String? = null,
)
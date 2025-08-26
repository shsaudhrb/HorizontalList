package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class Filters(
    @SerializedName("status_id") val statusId: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("assigned_agent_id") val assignedAgentId: String? = null,
    @SerializedName("partner_id") val partnerId: String? = null,
    @SerializedName("dc_id") val dcId: String? = null,
    val search: String? = null
)
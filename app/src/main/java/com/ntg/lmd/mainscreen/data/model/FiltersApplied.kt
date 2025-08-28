package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class FiltersApplied(
    @SerializedName("status_id")
    val statusId: String?,
    @SerializedName("customer_name")
    val customerName: String?,
    @SerializedName("order_number")
    val orderNumber: String?,
    @SerializedName("assigned_agent_id")
    val assignedAgentId: String?,
    @SerializedName("partner_id")
    val partnerId: String?,
    @SerializedName("dc_id")
    val dcId: String?,
    @SerializedName("search")
    val search: String?,
)

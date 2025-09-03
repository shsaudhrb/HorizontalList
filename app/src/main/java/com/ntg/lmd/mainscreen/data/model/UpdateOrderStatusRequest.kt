package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class UpdateOrderStatusRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("status_id") val statusId: Int,
    @SerializedName("assigned_agent_id") val assignedAgentId: String? = null,
)

data class UpdateOrderStatusEnvelope(
    val success: Boolean,
    val message: String?,
    val data: UpdatedOrderData?,
    @SerializedName("updated_by") val updatedBy: UpdatedByDto?,
)

data class UpdatedOrderData(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("status_id") val statusId: Int?,
    @SerializedName("assigned_agent_id") val assignedAgentId: String?,
    @SerializedName("last_updated") val lastUpdated: String?,
    @SerializedName("customer_name") val customerName: String?,
    val address: String?,
    @SerializedName("orderstatuses") val orderStatuses: OrderStatusDto?,
    @SerializedName("assigned_agent") val assignedAgent: AssignedAgentDto?,
    @SerializedName("previous_status_id") val previousStatusId: Int?,
    @SerializedName("status_changed") val statusChanged: Boolean?,
)

data class AssignedAgentDto(
    @SerializedName("id") val id: String?,
    @SerializedName("email") val email: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
)

data class UpdatedByDto(
    @SerializedName("user_id") val userId: String?,
    val email: String?,
    @SerializedName("full_name") val fullName: String?,
)

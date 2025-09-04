package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class OrdersEnvelope(
    val success: Boolean,
    val data: OrdersData? = null,
    val error: String? = null,
)

data class OrdersData(
    val orders: List<OrderDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

data class PaginationDto(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("total_pages") val totalPages: Int? = null,
    @SerializedName("total_count") val totalCount: Int? = null,
    val limit: Int? = null,
    @SerializedName("has_next_page") val hasNextPage: Boolean? = null,
    @SerializedName("has_prev_page") val hasPrevPage: Boolean? = null,
)

data class OrderDto(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("order_number")
    val orderNumber: String,
    @SerializedName("customer_id")
    val customerId: String?,
    @SerializedName("customer_name")
    val customerName: String?,
    val address: String?,
    @SerializedName("status_id")
    val statusId: Int?,
    @SerializedName("assigned_agent_id")
    val assignedAgentId: String?,
    @SerializedName("price")
    val price: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("partner_id")
    val partnerId: String?,
    @SerializedName("dc_id")
    val dcId: String?,
    @SerializedName("order_date")
    val orderDate: String?,
    @SerializedName("delivery_time")
    val deliveryTime: String?,
    @SerializedName("sla_met")
    val slaMet: String?,
    @SerializedName("serial_number")
    val serialNumber: String?,
    val coordinates: CoordinatesDto?,
    @SerializedName("last_updated")
    val lastUpdated: String?,
    val orderstatuses: OrderStatusDto?,
    val users: UserDto?,
    val partners: Any?,
    val distributioncenters: Any?,
    @SerializedName("distance_km")
    val distanceKm: Double?,
)

data class CoordinatesDto(
    val latitude: Double?,
    val longitude: Double?,
)

data class OrderStatusDto(
    @SerializedName("color_code")
    val colorCode: String?,
    @SerializedName("status_name")
    val statusName: String?,
    @SerializedName("font_color_code")
    val fontColorCode: String?,
)

data class UserDto(
    val id: String?,
    val email: String?,
    @SerializedName("full_name")
    val fullName: String?,
)

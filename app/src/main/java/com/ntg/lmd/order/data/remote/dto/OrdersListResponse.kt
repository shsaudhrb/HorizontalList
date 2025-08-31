package com.ntg.lmd.order.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrdersListResponse(
    val success: Boolean,
    val data: OrdersData,
    val error: String? = null,
)

data class OrdersData(
    val orders: List<OrderHistoryDto>,
    val pagination: Pagination,
)

// Which page you’re on right now, How many pages exist, and limit
data class Pagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("has_next_page") val hasNextPage: Boolean, // Continue until hasNextPage = false
    @SerializedName("has_prev_page") val hasPrevPage: Boolean,
)

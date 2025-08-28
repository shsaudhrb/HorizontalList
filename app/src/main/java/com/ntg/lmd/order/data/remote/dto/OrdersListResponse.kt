package com.ntg.lmd.order.data.remote.dto

data class OrdersListResponse(
    val success: Boolean,
    val data: OrdersData
)

data class OrdersData(
    val orders: List<OrderHistoryDto>,
    val pagination: Pagination
)

data class Pagination(
    val current_page: Int,
    val total_pages: Int,
    val total_count: Int,
    val limit: Int,
    val has_next_page: Boolean,
    val has_prev_page: Boolean
)

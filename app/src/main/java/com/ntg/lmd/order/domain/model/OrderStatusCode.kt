package com.ntg.lmd.order.domain.model

enum class OrderStatusCode(
    val code: Int,
) {
    CANCELLED(3),
    FAILED(7),
    DONE(8),
    ;

    companion object {
        fun fromList(statuses: List<OrderStatusCode>): String = statuses.joinToString(",") { it.code.toString() }
    }
}

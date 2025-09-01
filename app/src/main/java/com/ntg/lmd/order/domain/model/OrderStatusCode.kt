package com.ntg.lmd.order.domain.model

const val CANCELLED_CODE = 3
const val FAILED_CODE = 7
const val DONE_CODE = 8

enum class OrderStatusCode(
    val code: Int,
) {
    CANCELLED(CANCELLED_CODE),
    FAILED(FAILED_CODE),
    DONE(DONE_CODE),
    ;

    companion object {
        fun fromList(statuses: List<OrderStatusCode>): String = statuses.joinToString(",") { it.code.toString() }
    }
}

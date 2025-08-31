package com.ntg.lmd.mainscreen.domain.model

fun OrderStatus.toApiId(): Int = when (this) {
    OrderStatus.ADDED            -> 1
    OrderStatus.CONFIRMED        -> 2
    OrderStatus.PICKUP           -> 3
    OrderStatus.START_DELIVERY   -> 4
    OrderStatus.DELIVERY_DONE    -> 5
    OrderStatus.DELIVERY_FAILED  -> 6
    OrderStatus.CANCELED         -> 7
    OrderStatus.REASSIGNED       -> 8
}

fun apiIdToOrderStatus(id: Int?): OrderStatus = when (id) {
    1 -> OrderStatus.ADDED
    2 -> OrderStatus.CONFIRMED
    3 -> OrderStatus.PICKUP
    4 -> OrderStatus.START_DELIVERY
    5 -> OrderStatus.DELIVERY_DONE
    6 -> OrderStatus.DELIVERY_FAILED
    7 -> OrderStatus.CANCELED
    8 -> OrderStatus.REASSIGNED
    else -> OrderStatus.ADDED
}

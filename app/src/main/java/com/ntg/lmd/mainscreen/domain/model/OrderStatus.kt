package com.ntg.lmd.mainscreen.domain.model

fun OrderStatus.toApiId(): Int =
    when (this) {
        OrderStatus.ADDED           -> 1
        OrderStatus.CONFIRMED       -> 2
        OrderStatus.CANCELED        -> 3
        OrderStatus.REASSIGNED      -> 4
        OrderStatus.PICKUP          -> 5
        OrderStatus.START_DELIVERY  -> 6
        OrderStatus.DELIVERY_FAILED -> 7
        OrderStatus.DELIVERY_DONE   -> 8
    }

fun apiIdToOrderStatus(id: Int?): OrderStatus =
    when (id) {
        1 -> OrderStatus.ADDED
        2 -> OrderStatus.CONFIRMED
        3 -> OrderStatus.CANCELED
        4 -> OrderStatus.REASSIGNED
        5 -> OrderStatus.PICKUP
        6 -> OrderStatus.START_DELIVERY
        7 -> OrderStatus.DELIVERY_FAILED
        8 -> OrderStatus.DELIVERY_DONE
        else -> OrderStatus.ADDED
    }

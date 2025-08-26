package com.ntg.lmd.mainscreen.data.mapper

import com.ntg.lmd.mainscreen.data.model.OrderRecord
import com.ntg.lmd.mainscreen.domain.model.DeliveryLogDomain
import com.ntg.lmd.mainscreen.domain.model.DeliveryState
import java.util.Locale

fun OrderRecord.toDomain(): DeliveryLogDomain {
    val state =
        when (status.uppercase(Locale.getDefault())) {
            "DELIVERED" -> DeliveryState.DELIVERED
            "CANCELLED" -> DeliveryState.CANCELLED
            "FAILED" -> DeliveryState.FAILED
            else -> DeliveryState.OTHER
        }
    return DeliveryLogDomain(
        number = number,
        createdAtMillis = createdAtMillis,
        state = state,
    )
}

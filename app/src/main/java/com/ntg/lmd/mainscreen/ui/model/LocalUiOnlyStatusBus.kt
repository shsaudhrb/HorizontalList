package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import kotlinx.coroutines.flow.MutableSharedFlow

object LocalUiOnlyStatusBus {
    // Emits `(orderId, newStatus)` to reflect a local status change in the UI
    val statusEvents = MutableSharedFlow<Pair<String, OrderStatus>>(extraBufferCapacity = 8)

    // Emits `(message, retryAction)` for transient UI errors.
    val errorEvents = MutableSharedFlow<Pair<String, (() -> Unit)?>>(extraBufferCapacity = 8)
}

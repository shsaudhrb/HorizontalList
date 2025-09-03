package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.update
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus

class OrdersStatusViewModel(
    private val store: OrdersStore
) : ViewModel() {

    fun updateStatusLocally(
        id: String,
        newStatus: OrderStatus,
    ) {
        val updated =
            store.state.value.orders.map { o -> if (o.id == id) o.copy(status = newStatus) else o }
        store.state.update { it.copy(orders = updated) }
    }

    fun applyServerPatch(updated: OrderInfo) {
        // visible list
        val visible = store.state.value.orders.toMutableList()
        val i = visible.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            visible[i] = visible[i].copy(
                status = updated.status,
                details = updated.details ?: visible[i].details,
            )
            store.state.update { it.copy(orders = visible) }
        }

        val j = store.allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            store.allOrders[j] = store.allOrders[j].copy(
                status = updated.status,
                details = updated.details ?: store.allOrders[j].details,
            )
        }
    }
}

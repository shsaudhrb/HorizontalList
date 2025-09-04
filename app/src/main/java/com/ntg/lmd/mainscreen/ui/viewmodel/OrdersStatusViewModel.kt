package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import kotlinx.coroutines.flow.update

class OrdersStatusViewModel(
    private val store: OrdersStore,
) : ViewModel() {
    fun updateStatusLocally(
        id: String,
        newStatus: OrderStatus,
        newAssigneeId: String? = null,
    ) {
        val updatedVisible =
            store.state.value.orders.map { o ->
                if (o.id == id) {
                    o.copy(
                        status = newStatus,
                        assignedAgentId = newAssigneeId ?: o.assignedAgentId,
                    )
                } else {
                    o
                }
            }
        store.state.update { it.copy(orders = updatedVisible) }
        val j = store.allOrders.indexOfFirst { it.id == id }
        if (j != -1) {
            store.allOrders[j] =
                store.allOrders[j].copy(
                    status = newStatus,
                    assignedAgentId = newAssigneeId ?: store.allOrders[j].assignedAgentId,
                )
        }
        reapplyFilter()
    }

    fun applyServerPatch(updated: OrderInfo) {
        val visible =
            store.state.value.orders
                .toMutableList()
        val i = visible.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            visible[i] =
                visible[i].copy(
                    status = updated.status,
                    details = updated.details ?: visible[i].details,
                    assignedAgentId = updated.assignedAgentId,
                )
            store.state.update { it.copy(orders = visible) }
        }
        val j = store.allOrders.indexOfFirst { it.id == updated.id }
        if (j != -1) {
            store.allOrders[j] =
                store.allOrders[j].copy(
                    status = updated.status,
                    details = updated.details ?: store.allOrders[j].details,
                    assignedAgentId = updated.assignedAgentId,
                )
        }
        reapplyFilter()
    }

    private fun reapplyFilter() {
        val uid = store.currentUserId.value
        val q =
            store.state.value.query
                .trim()

        val before = store.allOrders.size
        val filtered =
            store.allOrders.filter { o ->
                val matchesQuery =
                    q.isBlank() ||
                        o.orderNumber.contains(q, true) ||
                        o.name.contains(q, true) ||
                        (o.details?.contains(q, true) == true)
                val matchesUser = uid.isNullOrBlank() || o.assignedAgentId == uid
                matchesQuery && matchesUser
            }

        android.util.Log.d("ReassignFlow", "reapplyFilter: uid=$uid, all=$before, filtered=${filtered.size}")

        val pageSize = com.ntg.lmd.mainscreen.domain.paging.OrdersPaging.PAGE_SIZE
        store.state.update { it.copy(orders = filtered.take(pageSize)) }
    }
}

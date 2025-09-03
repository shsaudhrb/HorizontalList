package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.paging.OrdersPaging
import kotlinx.coroutines.flow.update

class OrdersSearchViewModel(
    private val store: OrdersStore,
    private val pageSize: Int = OrdersPaging.PAGE_SIZE,
) : ViewModel() {

    fun applySearchQuery(raw: String) {
        val q = raw.trim()
        val base = currentFilteredFor(q, store.allOrders)

        val end = base.size <= pageSize
        val emptyMsg = if (base.isEmpty()) "No orders yet" else null

        store.state.update {
            it.copy(
                query = q,
                orders = base.take(pageSize),
                page = 1,
                endReached = end,
                emptyMessage = emptyMsg,
                isLoading = false,
                isLoadingMore = false,
                errorMessage = null,
            )
        }
    }

    private fun currentFilteredFor(
        queryRaw: String,
        all: List<OrderInfo>,
    ): List<OrderInfo> {
        val q = queryRaw.trim()
        if (q.isBlank()) return all
        return all.filter { o ->
            o.orderNumber.contains(q, ignoreCase = true) ||
                    o.name.contains(q, ignoreCase = true) ||
                    (o.details?.contains(q, ignoreCase = true) == true)
        }
    }
}

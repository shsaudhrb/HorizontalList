package com.ntg.lmd.mainscreen.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.ActionDialog
import com.ntg.lmd.mainscreen.ui.components.myOrderCard
import com.ntg.lmd.mainscreen.ui.model.MyOrderCardCallbacks
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.order.domain.model.PagingState
import com.ntg.lmd.order.domain.model.defaultVerticalListConfig
import com.ntg.lmd.order.ui.components.verticalListComponent

data class OrderListState(
    val orders: List<OrderInfo>,
    val listState: LazyListState,
    val isLoadingMore: Boolean,
    val updatingIds: Set<String>,
    val isRefreshing: Boolean,
    val endReached: Boolean,
)

data class OrderListCallbacks(
    val onReassignRequested: (String) -> Unit,
    val onDetails: (String) -> Unit,
    val onCall: (String) -> Unit,
    val onAction: (String, ActionDialog) -> Unit,
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
)

private val AutoHideOnSuccessStatuses = setOf(
    OrderStatus.DELIVERY_DONE,
    OrderStatus.DELIVERY_FAILED,
    OrderStatus.REASSIGNED,
)

@Composable
fun orderList(
    state: OrderListState,
    updateVm: UpdateOrderStatusViewModel,
    callbacks: OrderListCallbacks,
) {
    var hiddenIds by remember { mutableStateOf(emptySet<String>()) }


    LaunchedEffect(updateVm) {
        updateVm.success.collect { serverOrder ->
            if (serverOrder.status in AutoHideOnSuccessStatuses) {
                hiddenIds = hiddenIds + serverOrder.id
            }
        }
    }

    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing) hiddenIds = emptySet()
    }

    val filteredOrders by remember(state.orders, hiddenIds) {
        derivedStateOf {
            // VM already filtered by status + user
            state.orders.filter { it.id !in hiddenIds }
        }
    }

    Box(Modifier.padding(top = 12.dp)) {
        // applied generic list component for this screen
        verticalListComponent(
            items = filteredOrders,
            key = { it.id },
            itemContent = { order ->
                myOrderCard(
                    order = order,
                    isUpdating = state.updatingIds.contains(order.id),
                    callbacks = MyOrderCardCallbacks(
                        onReassignRequested = { callbacks.onReassignRequested(order.id) },
                        onDetails = { callbacks.onDetails(order.id) },
                        onCall = { callbacks.onCall(order.id) },
                        onAction = { act -> callbacks.onAction(order.id, act) },
                    ),
                    updateVm = updateVm,
                )
            },
            config =
                defaultVerticalListConfig(
                    listState = state.listState,
                    paging = PagingState(
                        isRefreshing = state.isRefreshing,
                        onRefresh = callbacks.onRefresh,
                        isLoadingMore = state.isLoadingMore,
                        endReached = state.endReached,
                        onLoadMore = callbacks.onLoadMore,
                    )
                ),
        )
    }
}

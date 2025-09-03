package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.OrderActions
import com.ntg.lmd.mainscreen.ui.components.myOrderCard
import com.ntg.lmd.mainscreen.ui.model.MyOrderCardCallbacks
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

data class OrderListState(
    val orders: List<OrderInfo>,
    val listState: LazyListState,
    val isLoadingMore: Boolean,
    val updatingIds: Set<String>,
    val isRefreshing: Boolean,
)

data class OrderListCallbacks(
    val onReassignRequested: (String) -> Unit,
    val onDetails: (String) -> Unit,
    val onCall: (String) -> Unit,
    val onAction: (String, OrderActions) -> Unit,
    val onRefresh: () -> Unit,
)

private val AutoHideOnSuccessStatuses =
    setOf(
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
    val hiddenIds = rememberHiddenIds(updateVm, state.isRefreshing)
    val filteredOrders = rememberFilteredOrders(state.orders, hiddenIds)

    ordersLazyList(
        filteredOrders = filteredOrders,
        listState = state.listState,
        isLoadingMore = state.isLoadingMore,
        updatingIds = state.updatingIds,
        updateVm = updateVm,
        callbacks = callbacks,
    )
}
@Composable
private fun rememberHiddenIds(
    updateVm: UpdateOrderStatusViewModel,
    isRefreshing: Boolean,
): Set<String> {
    var hiddenIds by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(updateVm) {
        updateVm.success.collect { serverOrder ->
            if (serverOrder.status in AutoHideOnSuccessStatuses) {
                hiddenIds = hiddenIds + serverOrder.id
            }
        }
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) hiddenIds = emptySet()
    }
    return hiddenIds
}

@Composable
private fun rememberFilteredOrders(
    orders: List<OrderInfo>,
    hiddenIds: Set<String>,
): List<OrderInfo> {
    return remember(orders, hiddenIds) {
        orders.filter { it.id !in hiddenIds } // VM already filtered by user/status
    }
}

@Composable
private fun ordersLazyList(
    filteredOrders: List<OrderInfo>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    updatingIds: Set<String>,
    updateVm: UpdateOrderStatusViewModel,
    callbacks: OrderListCallbacks,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(dimensionResource(R.dimen.mediumSpace)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.mediumSpace)),
    ) {
        items(items = filteredOrders, key = { it.id }) { order ->
            myOrderCard(
                order = order,
                isUpdating = updatingIds.contains(order.id),
                callbacks = toCardCallbacks(order.id, callbacks),
                updateVm = updateVm,
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.mediumSpace)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        item { Spacer(modifier = Modifier.height(dimensionResource(R.dimen.extraSmallSpace))) }
    }
}

private fun toCardCallbacks(
    orderId: String,
    callbacks: OrderListCallbacks,
) = MyOrderCardCallbacks(
    onReassignRequested = { callbacks.onReassignRequested(orderId) },
    onDetails = { callbacks.onDetails(orderId) },
    onCall = { callbacks.onCall(orderId) },
    onAction = { act -> callbacks.onAction(orderId, act) },
)

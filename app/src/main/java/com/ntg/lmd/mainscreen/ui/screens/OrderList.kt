package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.ntg.lmd.mainscreen.ui.components.myOrderCard
import com.ntg.lmd.mainscreen.ui.model.MyOrderCardCallbacks
import com.ntg.lmd.mainscreen.ui.model.OrderListCallbacks
import com.ntg.lmd.mainscreen.ui.model.OrderListState
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
    val onAction: (String, ActionDialog) -> Unit,
    val onRefresh: () -> Unit,
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
    LazyColumn(
        state = state.listState,
        contentPadding = PaddingValues(dimensionResource(R.dimen.mediumSpace)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.mediumSpace)),
    ) {
        items(items = filteredOrders, key = { it.id }) { order ->
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
        }
        if (state.isLoadingMore) {
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

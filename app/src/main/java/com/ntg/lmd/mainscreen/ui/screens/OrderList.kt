package com.ntg.lmd.mainscreen.ui.screens

import android.util.Log
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
import androidx.compose.runtime.collectAsState
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
import com.ntg.lmd.mainscreen.ui.components.ActionDialog
import com.ntg.lmd.mainscreen.ui.components.myOrderCard
import com.ntg.lmd.mainscreen.ui.model.MyOrderCardCallbacks
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

data class OrderListState(
    val orders: List<OrderInfo>,
    val listState: LazyListState,
    val isLoadingMore: Boolean,
    val updatingIds: Set<String>,
)

data class OrderListCallbacks(
    val onReassignRequested: (String) -> Unit,
    val onDetails: (String) -> Unit,
    val onCall: (String) -> Unit,
    val onAction: (String, ActionDialog) -> Unit,
    val onRefresh: () -> Unit,
)

@Composable
fun orderList(
    state: OrderListState,
    updateVm: UpdateOrderStatusViewModel,
    callbacks: OrderListCallbacks,
) {
    val myUserId by updateVm.currentUserId.collectAsState()
    var hiddenIds by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(updateVm, myUserId) {
        updateVm.success.collect { serverOrder ->
            val movedAway =
                myUserId != null &&
                    serverOrder.assignedAgentId != null &&
                    serverOrder.assignedAgentId != myUserId

            val shouldHide = serverOrder.status?.isTerminal() == true || movedAway
            if (shouldHide) hiddenIds = hiddenIds + serverOrder.id
            callbacks.onRefresh()
        }
    }

    val filteredOrders by remember(state.orders, myUserId, hiddenIds) {
        derivedStateOf {
            state.orders
                .asSequence()
                .filter { it.id !in hiddenIds }
                .filter { order -> !order.status?.isTerminal()!! && order.isMine(myUserId) }
                .toList()
        }
    }

    LaunchedEffect(state.orders, myUserId, hiddenIds, filteredOrders) {
        Log.d(
            "OrderListFilter",
            "me=$myUserId total=${state.orders.size}" +
                " hidden=${hiddenIds.size} filtered=${filteredOrders.size}",
        )
        filteredOrders.take(5).forEach { o ->
            Log.d("OrderListFilter", "id=${o.id} status=${o.status} assigned=${o.assignedAgentId}")
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
                callbacks =
                    MyOrderCardCallbacks(
                        onDetails = { callbacks.onDetails(order.id) },
                        onCall = { callbacks.onCall(order.id) },
                        onAction = { d -> callbacks.onAction(order.id, d) },
                        onReassignRequested = {
                            callbacks.onReassignRequested(order.id)
                        },
                    ),
                updateVm = updateVm,
            )
        }
        if (state.isLoadingMore) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.mediumSpace)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        item { Spacer(modifier = Modifier.height(dimensionResource(R.dimen.extraSmallSpace))) }
    }
}

private fun OrderStatus.isTerminal() = this == OrderStatus.CANCELED || this == OrderStatus.DELIVERY_DONE

private fun OrderInfo.isMine(myUserId: String?) = myUserId != null && assignedAgentId == myUserId

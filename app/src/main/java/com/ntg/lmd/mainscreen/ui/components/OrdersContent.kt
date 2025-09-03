package com.ntg.lmd.mainscreen.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.model.OrderListCallbacks
import com.ntg.lmd.mainscreen.ui.model.OrderListState
import com.ntg.lmd.mainscreen.ui.model.OrdersContentParams
import com.ntg.lmd.mainscreen.ui.screens.orderList
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

/*
private val AutoHideOnSuccessStatuses = setOf(
    OrderStatus.DELIVERY_DONE,
    OrderStatus.DELIVERY_FAILED,
    OrderStatus.REASSIGNED,
)*/
@Composable
fun ordersContent(params: OrdersContentParams) {
    val uiState by params.ordersVm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        when {
            params.state.isLoading && params.state.orders.isEmpty() -> loadingView()
            params.state.errorMessage != null ->
                errorView(params.state.errorMessage!!) { params.ordersVm.retry(params.context) }

            params.state.emptyMessage != null -> emptyView(params.state.emptyMessage!!)
            else -> showOrderList(uiState, params)
        }
    }
}

@Composable
private fun showOrderList(
    uiState: MyOrdersUiState,
    params: OrdersContentParams,
) {
    orderList(
        state =
            OrderListState(
                orders = uiState.orders,
                listState = params.listState,
                isLoadingMore = uiState.isLoadingMore,
                updatingIds = params.updatingIds,
                isRefreshing = uiState.isRefreshing,
            ),
        updateVm = params.updateVm,
        callbacks =
            OrderListCallbacks(
                onDetails = params.onOpenOrderDetails,
                onReassignRequested = params.onReassignRequested,
                onCall = { id -> handleCallClick(id, uiState, params.context) },
                onAction = { orderId, dialog ->
                    handleOrderAction(orderId, dialog, uiState, params.updateVm)
                },
                onRefresh = { params.ordersVm.refreshOrders() },
            ),
    )
}

private fun handleCallClick(
    id: String,
    uiState: MyOrdersUiState,
    context: Context,
) {
    val order = uiState.orders.firstOrNull { it.id == id }
    val phone = order?.customerPhone
    if (!phone.isNullOrBlank()) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } else {
        LocalUiOnlyStatusBus.errorEvents.tryEmit(context.getString(R.string.phone_missing) to null)
    }
}

private fun handleOrderAction(
    orderId: String,
    dialog: OrderActions,
    uiState: MyOrdersUiState,
    updateVm: UpdateOrderStatusViewModel,
) {
    val order = uiState.orders.firstOrNull { it.id == orderId }
    val label =
        when (dialog) {
            OrderActions.Confirm -> "Confirm"
            OrderActions.PickUp -> "PickUp"
            OrderActions.Start -> "StartDelivery"
            OrderActions.Deliver -> "Deliver"
            OrderActions.Fail -> "DeliveryFailed"
        }

    UpdateOrderStatusViewModel.OrderLogger.uiTap(orderId, order?.orderNumber, label)

    val status =
        when (dialog) {
            OrderActions.Confirm -> OrderStatus.CONFIRMED
            OrderActions.PickUp -> OrderStatus.PICKUP
            OrderActions.Start -> OrderStatus.START_DELIVERY
            OrderActions.Deliver -> OrderStatus.DELIVERY_DONE
            OrderActions.Fail -> OrderStatus.DELIVERY_FAILED
        }

    updateVm.update(orderId, status)
}

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

@Composable
fun ordersContent(params: OrdersContentParams) {
    val uiState by params.ordersVm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    ordersContentBody(
        uiState = uiState,
        params = params,
        context = context,
    )
}

@Composable
private fun ordersContentBody(
    uiState: MyOrdersUiState,
    params: OrdersContentParams,
    context: android.content.Context,
) {
    Column(Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.orders.isEmpty() ->
                loadingView()

            uiState.errorMessage != null ->
                errorView(uiState.errorMessage!!) { params.ordersVm.loadOrders(context) }

            uiState.emptyMessage != null ->
                emptyView(uiState.emptyMessage!!)

            else ->
                orderListHost(uiState = uiState, params = params, context = context)
        }
    }
}

@Composable
private fun orderListHost(
    uiState: MyOrdersUiState,
    params: OrdersContentParams,
    context: Context,
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
            makeOrderListCallbacks(
                uiState = uiState,
                params = params,
                context = context,
            ),
    )
}

private fun makeOrderListCallbacks(
    uiState: MyOrdersUiState,
    params: OrdersContentParams,
    context: Context,
): OrderListCallbacks =
    OrderListCallbacks(
        onDetails = params.onOpenOrderDetails,
        onReassignRequested = params.onReassignRequested,
        onCall = { id -> dialOrNotify(id, uiState, context) },
        onAction = actionHandler(uiState, params.updateVm),
        onRefresh = { params.ordersVm.refreshOrders() },
    )

private fun dialOrNotify(
    orderId: String,
    uiState: MyOrdersUiState,
    context: android.content.Context,
) {
    val order = uiState.orders.firstOrNull { it.id == orderId }
    val phone = order?.customerPhone
    if (!phone.isNullOrBlank()) {
        val intent =
            android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:$phone"),
            )
        context.startActivity(intent)
    } else {
        LocalUiOnlyStatusBus.errorEvents
            .tryEmit(context.getString(R.string.phone_missing) to null)
    }
}

private fun actionHandler(
    uiState: MyOrdersUiState,
    updateVm: UpdateOrderStatusViewModel,
): (String, OrderActions) -> Unit =
    { orderId, dialog ->
        val order = uiState.orders.firstOrNull { it.id == orderId }
        val (label, status) = dialogToLabelAndStatus(dialog)

        UpdateOrderStatusViewModel.OrderLogger
            .uiTap(orderId, order?.orderNumber, label)

        updateVm.update(orderId, status)
    }

private fun dialogToLabelAndStatus(dialog: OrderActions): Pair<String, OrderStatus> =
    when (dialog) {
        OrderActions.Confirm -> "Confirm" to OrderStatus.CONFIRMED
        OrderActions.PickUp -> "PickUp" to OrderStatus.PICKUP
        OrderActions.Start -> "StartDelivery" to OrderStatus.START_DELIVERY
        OrderActions.Deliver -> "Deliver" to OrderStatus.DELIVERY_DONE
        OrderActions.Fail -> "DeliveryFailed" to OrderStatus.DELIVERY_FAILED
    }

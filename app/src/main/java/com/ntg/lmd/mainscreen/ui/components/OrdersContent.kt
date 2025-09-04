package com.ntg.lmd.mainscreen.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.screens.OrderListCallbacks
import com.ntg.lmd.mainscreen.ui.screens.OrderListState
import com.ntg.lmd.mainscreen.ui.screens.orderList
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

data class OrdersContentDeps(
    val updateVm: UpdateOrderStatusViewModel,
    val listState: LazyListState,
    val updatingIds: Set<String>,
)

data class OrdersContentCallbacks(
    val onOpenOrderDetails: (String) -> Unit,
    val onReassignRequested: (String) -> Unit,
)

@Composable
fun ordersContent(
    ordersVm: MyOrdersViewModel,
    deps: OrdersContentDeps,
    cbs: OrdersContentCallbacks,
    modifier: Modifier = Modifier,
) {
    val uiState by ordersVm.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.orders.isEmpty() -> loadingView()
            uiState.errorMessage != null ->
                errorView(uiState.errorMessage!!) { ordersVm.listVM.retry(context) }

            uiState.emptyMessage != null -> emptyView(uiState.emptyMessage!!)
            else ->
                orderList(
                    state = buildOrderListState(uiState, deps),
                    updateVm = deps.updateVm,
                    callbacks =
                        buildOrderListCallbacks(
                            uiState = uiState,
                            deps = deps,
                            cbs = cbs,
                            context = context,
                            ordersVm = ordersVm,
                        ),
                )
        }
    }
}

private fun buildOrderListState(
    ui: MyOrdersUiState,
    deps: OrdersContentDeps,
) = OrderListState(
    orders = ui.orders,
    listState = deps.listState,
    isLoadingMore = ui.isLoadingMore,
    updatingIds = deps.updatingIds,
    isRefreshing = ui.isRefreshing,
    endReached = ui.endReached,
)

private fun buildOrderListCallbacks(
    uiState: MyOrdersUiState,
    deps: OrdersContentDeps,
    cbs: OrdersContentCallbacks,
    context: android.content.Context,
    ordersVm: MyOrdersViewModel,
) = OrderListCallbacks(
    onReassignRequested = cbs.onReassignRequested,
    onDetails = cbs.onOpenOrderDetails,
    onCall = { id ->
        val phone = uiState.orders.firstOrNull { it.id == id }?.customerPhone
        if (!phone.isNullOrBlank()) {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        } else {
            LocalUiOnlyStatusBus.errorEvents
                .tryEmit(context.getString(R.string.phone_missing) to null)
        }
    },
    onAction = { orderId, dialog ->
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
        when (dialog) {
            OrderActions.Confirm -> deps.updateVm.update(orderId, OrderStatus.CONFIRMED)
            OrderActions.PickUp -> deps.updateVm.update(orderId, OrderStatus.PICKUP)
            OrderActions.Start -> deps.updateVm.update(orderId, OrderStatus.START_DELIVERY)
            OrderActions.Deliver -> deps.updateVm.update(orderId, OrderStatus.DELIVERY_DONE)
            OrderActions.Fail -> deps.updateVm.update(orderId, OrderStatus.DELIVERY_FAILED)
        }
    },
    onRefresh = { ordersVm.listVM.refresh(context) },
    onLoadMore = { ordersVm.listVM.loadNextPage(context) },
)

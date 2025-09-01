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
import com.ntg.lmd.mainscreen.ui.screens.OrderListCallbacks
import com.ntg.lmd.mainscreen.ui.screens.OrderListState
import com.ntg.lmd.mainscreen.ui.screens.orderList
import com.ntg.lmd.mainscreen.ui.screens.orders.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

@Composable
 fun ordersContent(
    ordersVm: MyOrdersViewModel,
    updateVm: UpdateOrderStatusViewModel,
    state: MyOrdersUiState,
    listState: LazyListState,
    onOpenOrderDetails: (String) -> Unit,
    context: android.content.Context,
    updatingIds: Set<String>,
    onReassignRequested: (String) -> Unit,
) {
    // Collect once to avoid multiple recompositions in params
    val uiState by ordersVm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.orders.isEmpty() -> loadingView()
            state.errorMessage != null -> errorView(state.errorMessage!!) { ordersVm.retry(context) }
            state.emptyMessage != null -> emptyView(state.emptyMessage!!)
            else ->
                orderList(
                    state =
                        OrderListState(
                            orders = uiState.orders,
                            listState = listState,
                            isLoadingMore = uiState.isLoadingMore,
                            updatingIds = updatingIds,
                        ),
                    updateVm = updateVm,
                    callbacks =
                        OrderListCallbacks(
                            onDetails = onOpenOrderDetails,
                            onReassignRequested = onReassignRequested,
                            onCall = { id ->
                                val order = uiState.orders.firstOrNull { it.id == id }
                                val phone = order?.customerPhone
                                if (!phone.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } else {
                                    LocalUiOnlyStatusBus.errorEvents.tryEmit(
                                        context.getString(R.string.phone_missing) to null,
                                    )
                                }
                            },
                            onAction = { orderId, dialog ->
                                val order = uiState.orders.firstOrNull { it.id == orderId }
                                val label =
                                    when (dialog) {
                                        ActionDialog.Confirm -> "Confirm"
                                        ActionDialog.PickUp -> "PickUp"
                                        ActionDialog.Start -> "StartDelivery"
                                        ActionDialog.Deliver -> "Deliver"
                                        ActionDialog.Fail -> "DeliveryFailed"
                                    }
                                UpdateOrderStatusViewModel.OrderLogger.uiTap(orderId, order?.orderNumber, label)

                                when (dialog) {
                                    ActionDialog.Confirm -> updateVm.update(orderId, OrderStatus.CONFIRMED)
                                    ActionDialog.PickUp -> updateVm.update(orderId, OrderStatus.PICKUP)
                                    ActionDialog.Start -> updateVm.update(orderId, OrderStatus.START_DELIVERY)
                                    ActionDialog.Deliver -> updateVm.update(orderId, OrderStatus.DELIVERY_DONE)
                                    ActionDialog.Fail -> updateVm.update(orderId, OrderStatus.DELIVERY_FAILED)
                                }
                            },
                            onRefresh = { ordersVm.refreshOrders() }, // IMPORTANT: real reload
                        ),
                )
        }
    }
}
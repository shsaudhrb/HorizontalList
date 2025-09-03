package com.ntg.lmd.mainscreen.ui.model

import androidx.compose.foundation.lazy.LazyListState
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.OrderActions
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel.OrderLogger

data class OrdersContentParams(
    val ordersVm: MyOrdersViewModel,
    val updateVm: UpdateOrderStatusViewModel,
    val listState: LazyListState,
    val onOpenOrderDetails: (String) -> Unit,
    val updatingIds: Set<String>,
    val onReassignRequested: (String) -> Unit,
)

data class MyOrderCardCallbacks(
    val onDetails: () -> Unit,
    val onCall: () -> Unit,
    val onAction: (OrderActions) -> Unit,
    val onReassignRequested: () -> Unit,
)

data class OrderMenuCallbacks(
    val onDismiss: () -> Unit,
    val onPickUp: () -> Unit,
    val onCancel: () -> Unit,
    val onReassign: () -> Unit,
)

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

fun buildMenuCallbacks(
    order: OrderInfo,
    onDismiss: () -> Unit,
    onPickUp: () -> Unit,
    onCancel: () -> Unit,
    onReassign: () -> Unit,
): OrderMenuCallbacks =
    OrderMenuCallbacks(
        onDismiss = onDismiss,
        onPickUp = {
            onDismiss()
            OrderLogger.uiTap(order.id, order.orderNumber, "Menu:PickUp")
            onPickUp()
        },
        onCancel = {
            onDismiss()
            OrderLogger.uiTap(order.id, order.orderNumber, "Menu:Cancel")
            onCancel()
        },
        onReassign = {
            onDismiss()
            OrderLogger.uiTap(order.id, order.orderNumber, "Menu:Reassign")
            onReassign()
        },
    )

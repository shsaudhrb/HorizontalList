package com.ntg.lmd.mainscreen.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.OrdersUiConstants.CARD_ELEVATION
import com.ntg.lmd.mainscreen.ui.components.OrdersUiConstants.DETAILS_BUTTON_WEIGHT
import com.ntg.lmd.mainscreen.ui.components.OrdersUiConstants.OUTLINE_STROKE
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrderCardCallbacks
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

@Composable
fun myOrderCard(
    order: OrderInfo,
    isUpdating: Boolean,
    callbacks: MyOrderCardCallbacks,
    updateVm: UpdateOrderStatusViewModel,
) {
    var dialog by remember { mutableStateOf<OrderActions?>(null) }
    var showReassign by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(dimensionResource(R.dimen.largeSpace))) {
            myOrderCardHeader(order, isUpdating, callbacks.onReassignRequested, updateVm)
            Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))
            myOrderCardPrimaryRow(order.status, isUpdating, callbacks.onDetails) { dialog = it }
            Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
            myOrderCardFailIfNeeded(order.status, isUpdating) { dialog = OrderActions.Fail }
            callButton(callbacks.onCall)
        }
    }

    myOrderCardReassignDialog(show = showReassign, onClose = { showReassign = false })

    myOrderCardDialogsHost(
        dialog = dialog,
        onDismiss = { dialog = null },
        onConfirmForward = { act ->
            callbacks.onAction(act)
            dialog = null
        },
    )
}

@Composable
private fun myOrderCardHeader(
    order: OrderInfo,
    isUpdating: Boolean,
    onReassignRequested: () -> Unit,
    updateVm: UpdateOrderStatusViewModel,
) {
    orderHeaderWithMenu(
        order = order,
        enabled = !isUpdating,
        onPickUp = { updateVm.update(order.id, OrderStatus.PICKUP) },
        onCancel = { updateVm.update(order.id, OrderStatus.CANCELED) },
        onReassign = onReassignRequested,
    )
}

@Composable
private fun myOrderCardPrimaryRow(
    status: OrderStatus?,
    isUpdating: Boolean,
    onDetails: () -> Unit,
    setDialog: (OrderActions) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
        detailsButton(isUpdating, onDetails)

        actionForStatus(status)?.let { (labelRes, dialog) ->
            myOrderCardPrimaryAction(labelRes, isUpdating) { setDialog(dialog) }
        }
    }
}

@Composable
private fun RowScope.detailsButton(
    isUpdating: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isUpdating,
        modifier = Modifier.weight(DETAILS_BUTTON_WEIGHT),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(OUTLINE_STROKE, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
    ) {
        Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
        Text(
            text = stringResource(R.string.order_details),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )

}/*
        DetailsButton(isUpdating = isUpdating, onDetails = onDetails)
        PrimaryActionForStatus(status = status, isUpdating = isUpdating, setDialog = setDialog)
    }
}

@Composable
private fun RowScope.DetailsButton(isUpdating: Boolean, onDetails: () -> Unit) {
    OutlinedButton(
        onClick = onDetails,
        enabled = !isUpdating,
        modifier = Modifier.weight(DETAILS_BUTTON_WEIGHT),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(OUTLINE_STROKE, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
    ) {
        Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
        Text(
            text = stringResource(R.string.order_details),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )
    }
}*/
}
private fun actionForStatus(status: OrderStatus?): Pair<Int, OrderActions>? =
    when (status) {
        OrderStatus.ADDED -> R.string.confirm_order to OrderActions.Confirm
        OrderStatus.CONFIRMED -> R.string.pick_order to OrderActions.PickUp
        OrderStatus.PICKUP -> R.string.start_delivery to OrderActions.Start
        OrderStatus.START_DELIVERY -> R.string.deliver_order to OrderActions.Deliver
        else -> null
/*
@Composable
private fun RowScope.PrimaryActionForStatus(
    status: OrderStatus?,
    isUpdating: Boolean,
    setDialog: (OrderActions) -> Unit,
) {
    when (status) {
        OrderStatus.ADDED ->
            myOrderCardPrimaryAction(R.string.confirm_order, isUpdating) { setDialog(OrderActions.Confirm) }
        OrderStatus.CONFIRMED ->
            myOrderCardPrimaryAction(R.string.pick_order, isUpdating) { setDialog(OrderActions.PickUp) }
        OrderStatus.PICKUP ->
            myOrderCardPrimaryAction(R.string.start_delivery, isUpdating) { setDialog(OrderActions.Start) }
        OrderStatus.START_DELIVERY ->
            myOrderCardPrimaryAction(R.string.deliver_order, isUpdating) { setDialog(OrderActions.Deliver) }
        else -> Unit
    }
*/

@Composable
private fun RowScope.myOrderCardPrimaryAction(
    @StringRes labelRes: Int,
    isUpdating: Boolean,
    onClick: () -> Unit,
) {
    primaryActionButton(
        text = stringResource(labelRes),
        modifier = Modifier.weight(1f),
        enabled = !isUpdating,
        onClick = onClick,
    )
}

@Composable
private fun myOrderCardFailIfNeeded(
    status: OrderStatus?,
    isUpdating: Boolean,
    onFail: () -> Unit,
) {
    if (status == OrderStatus.PICKUP || status == OrderStatus.START_DELIVERY) {
        OutlinedButton(
            onClick = onFail,
            enabled = !isUpdating,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
        ) { Text(stringResource(R.string.delivery_failed)) }
    }
}

@Composable
private fun myOrderCardReassignDialog(
    show: Boolean,
    onClose: () -> Unit,
) {
    if (!show) return
    val reassignLabel = stringResource(R.string.reassign_order)
    reassignDialog(
        onDismiss = onClose,
        onConfirm = { assignee ->
            LocalUiOnlyStatusBus.errorEvents.tryEmit("$reassignLabel → $assignee" to null)
            onClose()
        },
    )
}

@Composable
private fun myOrderCardDialogsHost(
    dialog: OrderActions?,
    onDismiss: () -> Unit,
    onConfirmForward: (OrderActions) -> Unit,
) {
    when (dialog) {
        OrderActions.Confirm ->
            myOrderCardSimpleDialog(R.string.confirm_order, onDismiss) {
                onConfirmForward(OrderActions.Confirm)
            }
        OrderActions.PickUp ->
            myOrderCardSimpleDialog(R.string.pick_order, onDismiss) {
                onConfirmForward(OrderActions.PickUp)
            }
        OrderActions.Start ->
            myOrderCardSimpleDialog(R.string.start_delivery, onDismiss) {
                onConfirmForward(OrderActions.Start)
            }
        OrderActions.Deliver ->
            deliverDialog(onDismiss = onDismiss) {
                onConfirmForward(OrderActions.Deliver)
            }
        OrderActions.Fail ->
            reasonDialog(
                title = stringResource(R.string.delivery_failed),
                onDismiss = onDismiss,
            ) { onConfirmForward(OrderActions.Fail) }
        null -> Unit
    }
}

@Composable
private fun myOrderCardSimpleDialog(
    @StringRes titleRes: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    simpleConfirmDialog(
        title = stringResource(titleRes),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

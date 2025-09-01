package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.CARD_ELEVATION
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.DETAILS_BUTTON_WEIGHT
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.OUTLINE_STROKE
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel

@Composable
fun myOrderCard(
    order: OrderInfo,
    isUpdating: Boolean,
    onDetails: () -> Unit,
    onCall: () -> Unit,
    onAction: (ActionDialog) -> Unit,
    onReassignRequested: () -> Unit,
    updateVm: UpdateOrderStatusViewModel,
) {
    var dialog by remember { mutableStateOf<ActionDialog?>(null) }
    var showReassign by remember { mutableStateOf(false) }
    val reassignLabel = stringResource(R.string.reassign_order)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(dimensionResource(R.dimen.largeSpace))) {
            orderHeaderWithMenu(
                order = order,
                enabled = !isUpdating,
                onPickUp = {
                    updateVm.update(order.id, OrderStatus.PICKUP)
                },
                onCancel = {
                    updateVm.update(order.id, OrderStatus.CANCELED)
                },
                onReassign = { onReassignRequested() },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))

            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
                OutlinedButton(
                    onClick = onDetails,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(DETAILS_BUTTON_WEIGHT),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(OUTLINE_STROKE,
                        MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
                ) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
                    Text(text = stringResource(
                        R.string.order_details),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1)
                }

                when (order.status) {
                    OrderStatus.ADDED ->
                        primaryActionButton(
                            text = stringResource(R.string.confirm_order),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.Confirm },
                        )
                    OrderStatus.CONFIRMED ->
                        primaryActionButton(
                            text = stringResource(R.string.pick_order),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.PickUp },
                        )
                    OrderStatus.PICKUP ->
                        primaryActionButton(
                            text = stringResource(R.string.start_delivery),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.Start },
                        )
                    OrderStatus.START_DELIVERY ->
                        primaryActionButton(
                            text = stringResource(R.string.deliver_order),
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdating,
                            onClick = { dialog = ActionDialog.Deliver },
                        )
                    else -> {}
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))

            if (order.status == OrderStatus.PICKUP || order.status == OrderStatus.START_DELIVERY) {
                OutlinedButton(
                    onClick = { dialog = ActionDialog.Fail },
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
                ) { Text(stringResource(R.string.delivery_failed)) }
            }

            callButton(onCall)
        }
    }

    if (showReassign) {
        reassignDialog(
            onDismiss = { showReassign = false },
            onConfirm = { assignee ->
                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                    "$reassignLabel → $assignee" to null,
                )
                showReassign = false
            },
        )
    }

    when (dialog) {
        ActionDialog.Confirm ->
            simpleConfirmDialog(
                title = stringResource(R.string.confirm_order),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Confirm)
                    dialog = null
                },
            )
        ActionDialog.PickUp ->
            simpleConfirmDialog(
                title = stringResource(R.string.pick_order),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.PickUp)
                    dialog = null
                },
            )
        ActionDialog.Start ->
            simpleConfirmDialog(
                title = stringResource(R.string.start_delivery),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Start)
                    dialog = null
                },
            )
        ActionDialog.Deliver ->
            deliverDialog(
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Deliver)
                    dialog = null
                },
            )
        ActionDialog.Fail ->
            reasonDialog(
                title = stringResource(R.string.delivery_failed),
                onDismiss = { dialog = null },
                onConfirm = {
                    onAction(ActionDialog.Fail)
                    dialog = null
                },
            )
        null -> {}
    }
}
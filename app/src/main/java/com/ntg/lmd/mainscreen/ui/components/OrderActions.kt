package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.DETAILS_BUTTON_WEIGHT
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.OUTLINE_STROKE
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.ui.theme.SuccessGreen

sealed class ActionDialog {
    data object Confirm : ActionDialog()

    data object PickUp : ActionDialog()

    data object Start : ActionDialog()

    data object Deliver : ActionDialog()

    data object Fail : ActionDialog()
}


@Composable
fun actionPrimaryRow(
    status: OrderStatus,
    onDetails: () -> Unit,
    onAction: (ActionDialog) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
        OutlinedButton(
            onClick = onDetails,
            modifier = Modifier.weight(DETAILS_BUTTON_WEIGHT),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            border = BorderStroke(OUTLINE_STROKE, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
        ) {
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smallerSpace)))
            Text(text = stringResource(id = R.string.order_details), style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }

        when (status) {
            OrderStatus.ADDED ->
                primaryActionButton(
                    text = stringResource(R.string.confirm_order),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.Confirm) },
                )

            OrderStatus.CONFIRMED ->
                primaryActionButton(
                    text = stringResource(R.string.pick_order),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.PickUp) },
                )

            OrderStatus.PICKUP ->
                primaryActionButton(
                    text = stringResource(R.string.start_delivery),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.Start) },
                )

            OrderStatus.START_DELIVERY ->
                primaryActionButton(
                    text = stringResource(R.string.deliver_order),
                    modifier = Modifier.weight(1f),
                    onClick = { onAction(ActionDialog.Deliver) },
                )

            else -> {}
        }
    }
}

@Composable
fun secondaryFailRow(
    status: OrderStatus,
    onFailClick: () -> Unit,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
    if (status == OrderStatus.PICKUP || status == OrderStatus.START_DELIVERY) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace))) {
            OutlinedButton(
                onClick = onFailClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
            ) { Text(stringResource(R.string.delivery_failed)) }
        }
    }
}

@Composable
fun actionDialogs(
    dialog: ActionDialog?,
    orderId: String,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        ActionDialog.Confirm ->
            simpleConfirmDialog(
                title = stringResource(R.string.confirm_order),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.CONFIRMED)
                    onDismiss()
                },
            )

        ActionDialog.PickUp ->
            simpleConfirmDialog(
                title = stringResource(R.string.pick_order),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.PICKUP)
                    onDismiss()
                },
            )

        ActionDialog.Start ->
            simpleConfirmDialog(
                title = stringResource(R.string.start_delivery),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.START_DELIVERY)
                    onDismiss()
                },
            )

        ActionDialog.Deliver ->
            deliverDialog(
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.DELIVERY_DONE)
                    onDismiss()
                },
            )

        ActionDialog.Fail ->
            reasonDialog(
                title = stringResource(R.string.delivery_failed),
                onDismiss = onDismiss,
                onConfirm = {
                    LocalUiOnlyStatusBus.statusEvents.tryEmit(orderId to OrderStatus.DELIVERY_FAILED)
                    onDismiss()
                },
            )

        null -> {}
    }
}


@Composable
fun statusTint(status: String) =
    if (status.equals("confirmed", ignoreCase = true) ||
        status.equals("added", ignoreCase = true)
    ) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }

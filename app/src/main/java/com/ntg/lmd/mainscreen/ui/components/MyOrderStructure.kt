package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel.OrderLogger
import java.util.Locale

private const val KM_DIVISOR = 1000.0

@Composable
fun distanceBadge(
    distanceKm: Double,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary
    //  val value = distanceMeters?.div(KM_DIVISOR)
    Box(
        modifier =
            modifier
                .size(56.dp)
                .background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = distanceKm.let { String.format(Locale.getDefault(), "%.2f", it) },
                style = MaterialTheme.typography.labelSmall,
                color = fg,
            )
            Text(
                text = stringResource(R.string.unit_km),
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
fun primaryActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.mediumSpace)),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun callButton(onCall: () -> Unit) {
    Button(
        onClick = onCall,
        modifier = Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = stringResource(R.string.call),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(dimensionResource(R.dimen.drawer_icon_size)),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.smallerSpace)))
        Text(text = stringResource(R.string.call))
    }
}

@Composable
fun bottomStickyButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.mediumSpace),
                    vertical = dimensionResource(R.dimen.smallSpace),
                ),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) { Text(text = text, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun orderHeaderWithMenu(
    order: OrderInfo,
    enabled: Boolean = true,
    onPickUp: () -> Unit,
    onCancel: () -> Unit,
    onReassign: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        orderHeaderLeft(
            order = order,
            onPickUp = onPickUp,
            onCancel = onCancel,
            onReassign = onReassign,
            enabled = enabled,
        )
    }
}

@Composable
fun orderHeaderLeft(
    order: OrderInfo,
    onPickUp: () -> Unit,
    onCancel: () -> Unit,
    onReassign: () -> Unit,
    enabled: Boolean = true,
) {

    var menuExpanded by remember { mutableStateOf(false) }
    val statusEnum = order.status
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            distanceBadge(
                distanceKm = order.distanceKm,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.mediumSpace)),
            )
            Column {
                Text(order.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "#${order.orderNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                )
                Text(
                    text = order.status.toString(),
                    color = statusTint(order.status.toString()),
                    style = MaterialTheme.typography.titleSmall,
                )
                order.details?.let {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.extraSmallSpace)))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Column {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (statusEnum == OrderStatus.CONFIRMED) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pick_order)) },
                        enabled = enabled && order.status == OrderStatus.CONFIRMED,
                        onClick = {
                            menuExpanded = false
                            OrderLogger.uiTap(order.id, order.orderNumber, "Menu:PickUp")
                            onPickUp()
                        },
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cancel_order)) },
                        enabled = enabled && order.status in listOf(
                            OrderStatus.ADDED,
                            OrderStatus.CONFIRMED
                        ),
                        onClick = {
                            menuExpanded = false
                            OrderLogger.uiTap(order.id, order.orderNumber, "Menu:Cancel")
                            onCancel()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reassign_order)) },
                        enabled = enabled,
                        onClick = {
                            menuExpanded = false
                            OrderLogger.uiTap(order.id, order.orderNumber, "Menu:Reassign")
                            onReassign()
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.smallerSpace)))
            Text(text = order.price, style = MaterialTheme.typography.titleSmall)
        }
    }
}
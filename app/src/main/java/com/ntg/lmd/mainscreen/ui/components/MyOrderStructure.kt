package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.ui.model.OrderMenuCallbacks
import java.util.Locale

private const val KM_DIVISOR = 1000.0

@Composable
fun distanceBadge(
    distanceMeters: Double?,
    modifier: Modifier = Modifier,
) {
    val bg = MaterialTheme.colorScheme.primary
    val fg = MaterialTheme.colorScheme.onPrimary
    val value = distanceMeters?.div(KM_DIVISOR)
    Box(
        modifier =
            modifier
                .size(56.dp)
                .background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "--",
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
fun menuToggleButton(onOpen: () -> Unit) {
    IconButton(onClick = onOpen, modifier = Modifier.size(24.dp)) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.more_options),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun orderMenuSection(
    expanded: Boolean,
    order: OrderInfo,
    enabled: Boolean,
    callbacks: OrderMenuCallbacks,
) {
    orderMenu(
        expanded = expanded,
        order = order,
        enabled = enabled,
        callbacks = callbacks,
    )
}

@Composable
fun priceText(price: String) {
    Text(text = price, style = MaterialTheme.typography.titleSmall)
}

@Composable
fun orderMenu(
    expanded: Boolean,
    order: OrderInfo,
    enabled: Boolean,
    callbacks: OrderMenuCallbacks,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = callbacks.onDismiss) {
        if (order.status == OrderStatus.CONFIRMED) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pick_order)) },
                enabled = enabled && order.status == OrderStatus.CONFIRMED,
                onClick = callbacks.onPickUp,
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cancel_order)) },
                enabled = enabled && order.status in listOf(OrderStatus.ADDED, OrderStatus.CONFIRMED),
                onClick = callbacks.onCancel,
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reassign_order)) },
                enabled = enabled,
                onClick = callbacks.onReassign,
            )
        }
    }
}

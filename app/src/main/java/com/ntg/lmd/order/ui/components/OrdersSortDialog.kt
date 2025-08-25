package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.OrdersDialogsCallbacks
import com.ntg.lmd.order.domain.model.OrdersDialogsState

@Composable
fun ordersSortDialog(
    currentAscending: Boolean,
    onDismiss: () -> Unit,
    onApply: (Boolean) -> Unit,
) {
    var ageAsc by remember { mutableStateOf(currentAscending) }
    val pad = dimensionResource(R.dimen.mediumSpace)
    val gapSm = dimensionResource(R.dimen.smallSpace)
    val radius = dimensionResource(R.dimen.dialog_corner_radius)
    val elev = dimensionResource(R.dimen.elevation_small)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(radius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = elev,
        title = { Text(stringResource(R.string.sort), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ageAsc, onClick = { ageAsc = true })
                    Spacer(Modifier.width(gapSm))
                    Text(stringResource(R.string.sort_asc))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !ageAsc, onClick = { ageAsc = false })
                    Spacer(Modifier.width(gapSm))
                    Text(stringResource(R.string.sort_desc))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(ageAsc)
                onDismiss()
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.padding(horizontal = pad),
    )
}

@Composable
fun ordersHistoryDialogs(
    state: OrdersDialogsState,
    callbacks: OrdersDialogsCallbacks,
) {
    if (state.showFilter) {
        ordersFilterDialog(
            currentAllowed = state.filter.allowed,
            onDismiss = callbacks.onFilterDismiss,
            onApply = callbacks.onApplyFilter,
        )
    }
    if (state.showSort) {
        ordersSortDialog(
            currentAscending = state.filter.ageAscending,
            onDismiss = callbacks.onSortDismiss,
            onApply = callbacks.onApplySort,
        )
    }
}

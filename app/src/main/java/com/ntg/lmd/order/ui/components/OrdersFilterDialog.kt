package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.ntg.lmd.order.domain.model.OrderHistoryStatus

@Composable
fun ordersFilterDialog(
    currentAllowed: Set<OrderHistoryStatus>,
    onDismiss: () -> Unit,
    onApply: (Set<OrderHistoryStatus>) -> Unit,
) {
    var allowed by remember { mutableStateOf(currentAllowed) }
    val radius = dimensionResource(R.dimen.dialog_corner_radius)
    val elev = dimensionResource(R.dimen.elevation_small)
    val pad = dimensionResource(R.dimen.mediumSpace)
    val gapSm = dimensionResource(R.dimen.smallSpace)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(radius),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = elev,
        title = { Text(stringResource(R.string.filter), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.statuses), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(gapSm))

                statusCheckbox(
                    stringResource(R.string.delivered),
                    OrderHistoryStatus.DELIVERED,
                    allowed,
                ) { s, checked ->
                    allowed = if (checked) allowed + s else allowed - s
                }
                statusCheckbox(
                    stringResource(R.string.cancelled),
                    OrderHistoryStatus.CANCELLED,
                    allowed,
                ) { s, checked ->
                    allowed = if (checked) allowed + s else allowed - s
                }
                statusCheckbox(
                    stringResource(R.string.failed),
                    OrderHistoryStatus.FAILED,
                    allowed,
                ) { s, checked ->
                    allowed = if (checked) allowed + s else allowed - s
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(allowed)
                onDismiss()
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.padding(horizontal = pad),
    )
}

@Composable
private fun statusCheckbox(
    label: String,
    status: OrderHistoryStatus,
    selected: Set<OrderHistoryStatus>,
    onChange: (OrderHistoryStatus, Boolean) -> Unit,
) {
    val checked = status in selected
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { onChange(status, it) })
        Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
        Text(label)
    }
}

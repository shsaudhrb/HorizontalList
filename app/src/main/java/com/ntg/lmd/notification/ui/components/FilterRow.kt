package com.ntg.lmd.notification.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import com.ntg.lmd.notification.domain.model.NotificationFilter

@Composable
fun filterRow(
    filter: NotificationFilter,
    onFilterChange: (NotificationFilter) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace)),
    ) {
        @Composable
        fun chip(
            label: String,
            value: NotificationFilter,
        ) {
            val selected = filter == value
            FilterChip(
                selected = selected,
                onClick = { onFilterChange(value) },
                label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.chipRoundCorner)),
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                        borderWidth = dimensionResource(R.dimen.smallestStrokeWidth),
                        selectedBorderWidth = dimensionResource(R.dimen.smallStrokeWidth),
                    ),
            )
        }

        chip(stringResource(R.string.filter_all), NotificationFilter.All)
        chip(stringResource(R.string.filter_orders), NotificationFilter.Orders)
        chip(stringResource(R.string.filter_wallet), NotificationFilter.Wallet)
        chip(stringResource(R.string.filter_other), NotificationFilter.Other)
    }
}

@Composable
fun notificationPlaceholder() {
    val height = dimensionResource(R.dimen.notificationPlaceholderHeight) // 72dp
    val radius = dimensionResource(R.dimen.card_radius) // 12dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

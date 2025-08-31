package com.ntg.lmd.order.ui.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.ui.screen.statusBadge
import com.ntg.lmd.ui.theme.ErrorRed
import com.ntg.lmd.ui.theme.PresentGreen
import com.ntg.lmd.utils.timeHelper

@SuppressLint("DefaultLocale")
@Composable
fun orderHistoryCard(
    context: Context,
    order: OrderHistoryUi,
) {
    val pad = dimensionResource(R.dimen.mediumSpace)
    val radius = dimensionResource(R.dimen.card_radius)
    val elev = dimensionResource(R.dimen.elevation_small)
    val hair = dimensionResource(R.dimen.hairline)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elev),
        border = BorderStroke(hair, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(pad)) {
            orderHeaderRow(order)
            Spacer(Modifier.height(4.dp))
            orderFooterRow(context, order)
        }
    }
}

@Composable
private fun orderHeaderRow(order: OrderHistoryUi) {
    val currency = stringResource(R.string.currency_egp)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(order.number, color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${order.total} $currency",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun orderFooterRow(
    context: Context,
    order: OrderHistoryUi,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(order.customer, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = timeHelper(context, order.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            order.isCancelled -> statusBadge("Cancelled", ErrorRed)
            order.isFailed -> statusBadge("Failed", MaterialTheme.colorScheme.onSurfaceVariant)
            order.isDelivered -> statusBadge("Delivered", PresentGreen)
        }
    }
}

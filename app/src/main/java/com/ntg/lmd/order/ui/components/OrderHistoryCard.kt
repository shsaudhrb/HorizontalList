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
import androidx.compose.foundation.layout.width
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
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.utils.timeHelper

@SuppressLint("DefaultLocale")
@Composable
fun orderHistoryCard(
    context: Context,
    order: OrderHistoryUi,
) {
    val pad = dimensionResource(R.dimen.mediumSpace)
    val gapSm = dimensionResource(R.dimen.smallSpace)
    val gapXs = dimensionResource(R.dimen.smallerSpace)
    val radius = dimensionResource(R.dimen.card_radius)
    val hair = dimensionResource(R.dimen.hairline)
    val elev = dimensionResource(R.dimen.elevation_small)
    val currency = stringResource(R.string.currency_egp)
    val gapV = dimensionResource(R.dimen.smallestSpace)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elev),
        border = BorderStroke(hair, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(pad)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    order.number,
                    color = Color.Gray,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = order.total.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(gapV))
                    Text(
                        currency,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(gapXs))
            Text(order.customer, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(gapV))
            Text(
                text = timeHelper(context, order.createdAtMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(gapSm))
        }
    }
}

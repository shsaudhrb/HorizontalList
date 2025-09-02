package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import java.util.Locale

@Composable
fun orderHeader(order: OrderInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        distanceBadge(order.distanceKm)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = order.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${order.orderNumber} - ${order.timeAgo}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// @Composable
// fun orderCard(
//    order: OrderInfo,
//    onAddClick: (OrderInfo) -> Unit,
//    onOrderClick: (OrderInfo) -> Unit,
//    modifier: Modifier = Modifier,
// ) {
//    Card(
//        onClick = { onOrderClick(order) },
//        shape = RoundedCornerShape(14.dp),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//        modifier = modifier.size(width = 270.dp, height = 150.dp),
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                // distance
//                Box(
//                    modifier = Modifier
//                        .size(50.dp)
//                        .clip(CircleShape)
//                        .background(MaterialTheme.colorScheme.primary),
//                    contentAlignment = Alignment.Center,
//                ) {
//                    Text(
//                        text = String.format(Locale.US, "%.1fkm", order.distanceKm),
//                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        maxLines = 1,
//                    )
//                }
//
//                Spacer(Modifier.width(8.dp))
//
//                // ---- order name, number, and time ago ----
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = order.name,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.primary,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis,
//                    )
//                    Text(
//                        text = "${order.orderNumber} - ${order.timeAgo}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis,
//                    )
//                }
//            }
//        }
//    }
// }

@Composable
private fun distanceBadge(km: Double) {
    Box(
        modifier =
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = String.format(Locale.US, "%.1fkm", km),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

@Composable
fun orderItemsCount(count: Int) {
    Text(
        text = "Items in order ($count)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun orderAddButton(
    order: OrderInfo,
    onAddClick: (OrderInfo) -> Unit,
) {
    Button(onClick = { onAddClick(order) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.add_to_your_orders), maxLines = 1)
    }
}

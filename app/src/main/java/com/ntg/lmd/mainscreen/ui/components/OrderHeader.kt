package com.ntg.lmd.mainscreen.ui.components
//
// import androidx.compose.foundation.layout.Arrangement
// import androidx.compose.foundation.layout.Column
// import androidx.compose.foundation.layout.Row
// import androidx.compose.foundation.layout.Spacer
// import androidx.compose.foundation.layout.fillMaxWidth
// import androidx.compose.foundation.layout.height
// import androidx.compose.foundation.layout.padding
// import androidx.compose.material3.MaterialTheme
// import androidx.compose.material3.Text
// import androidx.compose.runtime.Composable
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.mutableStateOf
// import androidx.compose.runtime.remember
// import androidx.compose.runtime.setValue
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.res.dimensionResource
// import com.ntg.lmd.R
// import com.ntg.lmd.mainscreen.domain.model.OrderInfo
// import com.ntg.lmd.mainscreen.ui.model.buildMenuCallbacks
//
// @Composable
// fun orderHeaderWithMenu(
//    order: OrderInfo,
//    enabled: Boolean = true,
//    onPickUp: () -> Unit,
//    onCancel: () -> Unit,
//    onReassign: () -> Unit,
// ) {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Top,
//    ) {
//        orderHeaderLeft(
//            order = order,
//            onPickUp = onPickUp,
//            onCancel = onCancel,
//            onReassign = onReassign,
//            enabled = enabled,
//        )
//    }
// }
//
// @Composable
// fun orderHeaderLeft(
//    order: OrderInfo,
//    onPickUp: () -> Unit,
//    onCancel: () -> Unit,
//    onReassign: () -> Unit,
//    enabled: Boolean = true,
// ) {
//    headerRow(
//        order = order,
//        enabled = enabled,
//        onPickUp = onPickUp,
//        onCancel = onCancel,
//        onReassign = onReassign,
//    )
// }
//
// @Composable
// fun headerRow(
//    order: OrderInfo,
//    enabled: Boolean,
//    onPickUp: () -> Unit,
//    onCancel: () -> Unit,
//    onReassign: () -> Unit,
// ) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween,
//    ) {
//        leftBlock(order)
//        rightMenuBlock(
//            order = order,
//            enabled = enabled,
//            onPickUp = onPickUp,
//            onCancel = onCancel,
//            onReassign = onReassign,
//        )
//    }
// }
//
// @Composable
// fun leftBlock(order: OrderInfo) {
//    Row(verticalAlignment = Alignment.CenterVertically) {
//        distanceBadge(
//            distanceKm = order.distanceKm,
//            modifier = Modifier.padding(end = dimensionResource(R.dimen.mediumSpace)),
//        )
//        Column {
//            Text(order.name, style = MaterialTheme.typography.titleMedium)
//            Text(
//                "#${order.orderNumber}",
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
//                maxLines = 1,
//            )
//            Text(
//                text = order.status.toString(),
//                color = statusTint(order.status.toString()),
//                style = MaterialTheme.typography.titleSmall,
//            )
//            orderDetails(order.details)
//        }
//    }
// }
//
// @Composable
// fun orderDetails(details: String?) {
//    details ?: return
//    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.extraSmallSpace)))
//    Text(details, style = MaterialTheme.typography.bodySmall)
// }
//
// @Composable
// fun rightMenuBlock(
//    order: OrderInfo,
//    enabled: Boolean,
//    onPickUp: () -> Unit,
//    onCancel: () -> Unit,
//    onReassign: () -> Unit,
// ) {
//    var menuExpanded by remember { mutableStateOf(false) }
//
//    Column(horizontalAlignment = Alignment.End) {
//        menuToggleButton { menuExpanded = true }
//
//        val callbacks =
//            remember(order, enabled, onPickUp, onCancel, onReassign) {
//                buildMenuCallbacks(
//                    order = order,
//                    onDismiss = { menuExpanded = false },
//                    onPickUp = onPickUp,
//                    onCancel = onCancel,
//                    onReassign = onReassign,
//                )
//            }
//        orderMenuSection(
//            expanded = menuExpanded,
//            order = order,
//            enabled = enabled,
//            callbacks = callbacks,
//        )
//
//        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.smallerSpace)))
//        priceText(order.price)
//    }
// }

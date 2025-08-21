package com.ntg.lmd.mainscreen.ui.screens.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.ui.components.emptyView
import com.ntg.lmd.mainscreen.ui.components.errorView
import com.ntg.lmd.mainscreen.ui.components.infoBanner
import com.ntg.lmd.mainscreen.ui.components.loadingView
import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderUI
import com.ntg.lmd.ui.theme.DeepRed
import com.ntg.lmd.ui.theme.SuccessGreen
import com.ntg.lmd.ui.theme.White

@Composable
fun myOrdersScreen(
    navController: NavController,
    externalQuery: String,
) {
    val vm: MyOrdersViewModel = viewModel()
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.loadOrders() }
    LaunchedEffect(externalQuery) { vm.onQueryChange(externalQuery) }
    Column(modifier = Modifier.fillMaxSize()) {
        if (!state.isGpsAvailable) {
            infoBanner(
                text = stringResource(R.string.distance_unavailable_gps),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        when {
            state.isLoading -> loadingView()
            state.errorMessage != null -> errorView(state.errorMessage!!, vm::retry)
            state.emptyMessage != null -> emptyView(state.emptyMessage!!)
            else ->
                orderList(
                    orders = state.orders,
                    onDetails = { /* navController.navigate(...) */ },
                    onConfirmOrPick = { id -> },
                    onCall = { /* TODO: call intent */ },
                )
        }
    }
}

@Composable
fun orderList(
    orders: List<OrderUI>,
    onDetails: (Long) -> Unit,
    onConfirmOrPick: (Long) -> Unit,
    onCall: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = orders, key = { it.id }) { order ->
            orderCard(
                order = order,
                onDetails = { onDetails(order.id) },
                onConfirmOrPick = { onConfirmOrPick(order.id) },
                onCall = { onCall(order.id) },
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
fun orderCard(
    order: OrderUI,
    onDetails: () -> Unit,
    onConfirmOrPick: () -> Unit,
    onCall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: left info, right status (on top) + price + distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "#${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                    )
                    Text(order.customerName, style = MaterialTheme.typography.bodyMedium)
                    order.details?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = order.status,
                        color = statusTint(order.status),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format("%.2f", order.totalPrice),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            order.distanceMeters?.let { (it / 1000.0).formatKm() }
                                ?: stringResource(R.string.distance_na),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.End,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row (Details + Confirm/Pick)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDetails,
                    modifier = Modifier.weight(1f),
                ) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(id = R.string.order_details))
                }

                Button(
                    onClick = onConfirmOrPick,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = DeepRed,
                            contentColor = White,
                        ),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = actionLabel(order.status))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCall,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = DeepRed,
                        contentColor = White,
                    ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Call",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(id = R.string.call))
            }
        }
    }
}

@Composable
private fun statusTint(status: String) =
    if (status.equals("confirmed", ignoreCase = true) ||
        status.equals("added", ignoreCase = true)
    ) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun actionLabel(status: String): String =
    if (status.equals("added", ignoreCase = true)) {
        stringResource(R.string.confirm_order)
    } else {
        stringResource(R.string.pick_order)
    }

private fun Double.formatKm(): String = String.format("%.1f km", this)

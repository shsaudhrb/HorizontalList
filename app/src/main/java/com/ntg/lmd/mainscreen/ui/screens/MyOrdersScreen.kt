package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
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
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.ui.theme.DeepRed
import com.ntg.lmd.ui.theme.SuccessGreen
import com.ntg.lmd.ui.theme.White
import java.util.Locale

private const val VISIBLE_THRESHOLD = 3
private const val KM_DIVISOR = 1000.0
private val CARD_ELEVATION = 3.dp

@Suppress("UnusedParameter")
@Composable
fun myOrdersScreen(
    navController: NavController,
    externalQuery: String,
) {
    val vm: MyOrdersViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadOrders(context) }
    LaunchedEffect(externalQuery) { vm.onQueryChange(externalQuery) }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - VISIBLE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) vm.loadNextPage() }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!state.isGpsAvailable) {
            infoBanner(
                text = stringResource(R.string.distance_unavailable_gps),
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space_screen_padding)),
            )
        }

        when {
            state.isLoading -> loadingView()
            state.errorMessage != null -> errorView(state.errorMessage!!, { vm.retry(context) })
            state.emptyMessage != null -> emptyView(state.emptyMessage!!)
            else ->
                orderList(
                    orders = state.orders,
                    listState = listState,
                    actions =
                        OrderActions(
                            onDetails = { TODO("Implement navigation to order details") },
                            onConfirmOrPick = { id -> },
                            onCall = { TODO("Implement call intent") },
                        ),
                    isLoadingMore = state.isLoadingMore,
                )
        }
    }
}

@Composable
fun orderList(
    orders: List<OrderUI>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    actions: OrderActions,
    isLoadingMore: Boolean,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(dimensionResource(R.dimen.space_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space_large)),
    ) {
        items(items = orders, key = { it.id }) { order ->
            orderCard(
                order = order,
                onDetails = { actions.onDetails(order.id) },
                onConfirmOrPick = { actions.onConfirmOrPick(order.id) },
                onCall = { actions.onCall(order.id) },
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.space_screen_padding)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
        item { Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space_xsmall))) }
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
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        colors = CardDefaults.cardColors(containerColor = White),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.space_xlarge))) {
            orderHeader(order)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space_large)))
            orderActionsRow(
                onDetails = onDetails,
                onConfirmOrPick = onConfirmOrPick,
                status = order.status,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space_medium)))
            callButton(onCall)
        }
    }
}

@Composable
private fun orderHeader(order: OrderUI) {
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
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space_xsmall)))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = order.status,
                color = statusTint(order.status),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space_small)))
            Text(
                text = String.format(Locale.getDefault(), "%.2f", order.totalPrice),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    order.distanceMeters?.let { (it / KM_DIVISOR).formatKm() }
                        ?: stringResource(R.string.distance_na),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun orderActionsRow(
    onDetails: () -> Unit,
    onConfirmOrPick: () -> Unit,
    status: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space_small))) {
        OutlinedButton(
            onClick = onDetails,
            modifier = Modifier.weight(1f),
        ) {
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space_small)))
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
            shape = RoundedCornerShape(dimensionResource(R.dimen.space_large)),
        ) {
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space_small)))
            Text(text = actionLabel(status))
        }
    }
}

@Composable
private fun callButton(onCall: () -> Unit) {
    Button(
        onClick = onCall,
        modifier = Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = DeepRed,
                contentColor = White,
            ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius)),
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = "Call",
            modifier = Modifier.size(dimensionResource(R.dimen.drawer_icon_size)),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space_small)))
        Text(text = stringResource(R.string.call))
    }
}

data class OrderActions(
    val onDetails: (Long) -> Unit,
    val onConfirmOrPick: (Long) -> Unit,
    val onCall: (Long) -> Unit,
)

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

private fun Double.formatKm(): String = String.format(Locale.getDefault(), "%.1f km", this)

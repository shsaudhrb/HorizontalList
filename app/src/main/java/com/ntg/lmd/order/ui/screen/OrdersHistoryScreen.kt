package com.ntg.lmd.order.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.OrderUi
import com.ntg.lmd.order.ui.components.exportOrdersPdf
import com.ntg.lmd.order.ui.components.ordersHistoryMenu
import com.ntg.lmd.order.ui.components.sharePdf
import com.ntg.lmd.order.ui.components.timeHelper
import com.ntg.lmd.order.ui.viewmodel.OrdersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("DefaultLocale")
@Composable
private fun orderCard(
    context: Context,
    order: OrderUi,
) {
    val pad = dimensionResource(R.dimen.text_spacing_medium)
    val radius = dimensionResource(R.dimen.card_radius)
    val hair = dimensionResource(R.dimen.hairline)
    val gapXs = dimensionResource(R.dimen.space_xsmall)
    val gapSm = dimensionResource(R.dimen.space_small)
    val elev = dimensionResource(R.dimen.elevation_small)
    val currency = stringResource(R.string.currency_egp)
    val gapV = dimensionResource(R.dimen.group_vertical_padding)
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

@Composable
fun ordersHistoryRoute(registerOpenMenu: ((() -> Unit) -> Unit)? = null) {
    val vm: OrdersViewModel = viewModel()
    val orders by vm.orders.collectAsState(emptyList())
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { registerOpenMenu?.invoke { menuOpen = true } }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_spacing)),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.text_spacing_medium)),
    ) {
        items(orders, key = { it.number }) {
            orderCard(ctx, order = it)
        }
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        ordersHistoryMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            onExportPdf = {
                scope.launch(Dispatchers.IO) {
                    exportOrdersPdf(ctx, orders)?.let { file ->
                        withContext(Dispatchers.Main) {
                            sharePdf(ctx, file)
                        }
                    }
                }
            },
            onFilter = {},
            onSort = {},
        )
    }
}

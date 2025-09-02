package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.model.BottomCallbacks
import com.ntg.lmd.mainscreen.ui.model.BottomDeps
import java.util.Locale

@Composable
fun customBottom(
    orders: List<OrderInfo>,
    selectedOrderNumber: String?,
    onAddClick: (OrderInfo) -> Unit = {},
    onOrderClick: (OrderInfo) -> Unit = {},
    onCenteredOrderChange: (OrderInfo, Int) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()
    val sidePadding = rememberSidePadding()
    val px = with(LocalDensity.current) { sidePadding.roundToPx() }

    val deps = remember(orders, listState, sidePadding, px) { BottomDeps(orders, listState, sidePadding, px) }
    val callbacks =
        remember(onAddClick, onOrderClick, onCenteredOrderChange) {
            BottomCallbacks(onAddClick, onOrderClick, onCenteredOrderChange)
        }

    customBottomBody(deps = deps, callbacks = callbacks, selectedOrderNumber = selectedOrderNumber)
}

@Composable
private fun customBottomBody(
    deps: BottomDeps,
    callbacks: BottomCallbacks,
    selectedOrderNumber: String?,
) {
    var lastCentered by remember { mutableIntStateOf(-1) }
    var programmatic by remember { mutableStateOf(false) }

    jumpToSelectedOrderEffect(
        selectedOrderNumber = selectedOrderNumber,
        deps = deps,
        setProgrammatic = { programmatic = it },
        onJumped = { idx -> lastCentered = idx },
    )

    notifyCenteredOnIdleEffect(
        deps = deps,
        programmatic = programmatic,
        lastCentered = lastCentered,
        setLastCentered = { lastCentered = it },
        onCentered = callbacks.onCentered,
    )

    bottomContainer {
        ordersRow(
            deps = deps,
            setProgrammatic = { programmatic = it },
            onSnapAndCenter = { idx ->
                lastCentered = idx
                callbacks.onCentered(deps.orders[idx], idx)
            },
            callbacks = callbacks,
        )
    }
}

@Composable
private fun jumpToSelectedOrderEffect(
    selectedOrderNumber: String?,
    deps: BottomDeps,
    setProgrammatic: (Boolean) -> Unit,
    onJumped: (Int) -> Unit,
) {
    LaunchedEffect(selectedOrderNumber, deps.orders) {
        val orders = deps.orders
        if (!selectedOrderNumber.isNullOrEmpty() && orders.isNotEmpty()) {
            val i = orders.indexOfFirst { it.orderNumber == selectedOrderNumber }
            if (i >= 0) {
                setProgrammatic(true)
                try {
                    deps.listState.animateScrollToItem(i, -deps.px)
                    onJumped(i)
                } finally {
                    setProgrammatic(false)
                }
            }
        }
    }
}

@Composable
private fun notifyCenteredOnIdleEffect(
    deps: BottomDeps,
    programmatic: Boolean,
    lastCentered: Int,
    setLastCentered: (Int) -> Unit,
    onCentered: (OrderInfo, Int) -> Unit,
) {
    LaunchedEffect(deps.orders, deps.listState, programmatic, lastCentered) {
        snapshotFlow { deps.listState.isScrollInProgress }.collect { moving ->
            if (!moving && !programmatic && deps.orders.isNotEmpty()) {
                val info = deps.listState.layoutInfo
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                val nearest =
                    info.visibleItemsInfo.minByOrNull {
                        kotlin.math.abs((it.offset + it.size / 2) - center)
                    } ?: return@collect
                val idx = nearest.index
                if (idx in deps.orders.indices && idx != lastCentered) {
                    setLastCentered(idx)
                    onCentered(deps.orders[idx], idx)
                }
            }
        }
    }
}

@Composable
private fun bottomContainer(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.primary),
    ) { content() }
}

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
fun orderCard(
    order: OrderInfo,
    onAddClick: (OrderInfo) -> Unit,
    onOrderClick: (OrderInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onOrderClick(order) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.size(width = 270.dp, height = 150.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // distance
                Box(
                    modifier =
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fkm", order.distanceKm),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.width(8.dp))

                // ---- order name, number, and time ago ----
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

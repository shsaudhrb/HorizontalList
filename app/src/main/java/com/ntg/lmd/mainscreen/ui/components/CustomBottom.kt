package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Composable
fun customBottom(
    orders: List<OrderInfo>,
    selectedOrderNumber: String?,
    onAddClick: (OrderInfo) -> Unit = {},
    onOrderClick: (OrderInfo) -> Unit = {},
    onCenteredOrderChange: (OrderInfo, Int) -> Unit = { _, _ -> },
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sidePadding =
        ((LocalConfiguration.current.screenWidthDp.dp - dimensionResource(id = R.dimen.orders_card_width)) / 2)
            .coerceAtLeast(0.dp)
    var lastCentered by remember { mutableIntStateOf(-1) }
    var programmatic by remember { mutableStateOf(false) }
    val px = with(LocalDensity.current) { sidePadding.roundToPx() }

    // Jump to the externally selected order
    LaunchedEffect(selectedOrderNumber, orders) {
        if (!selectedOrderNumber.isNullOrEmpty() && orders.isNotEmpty()) {
            val i = orders.indexOfFirst { it.orderNumber == selectedOrderNumber }
            if (i >= 0) {
                programmatic = true
                try {
                    listState.animateScrollToItem(i, -px)
                    lastCentered = i
                } finally {
                    programmatic = false
                }
            }
        }
    }

    LaunchedEffect(orders, listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { moving ->
            if (!moving && !programmatic && orders.isNotEmpty()) {
                val info = listState.layoutInfo
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                val nearest =
                    info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - center) }
                        ?: return@collect
                val idx = nearest.index
                if (idx in orders.indices && idx != lastCentered) {
                    lastCentered = idx
                    onCenteredOrderChange(orders[idx], idx)
                }
            }
        }
    }

    if (orders.isEmpty()) return

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.primary),
    ) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = sidePadding, end = sidePadding),
        ) {
            itemsIndexed(orders, key = { _, order -> order.orderNumber }) { index, order ->
                orderCard(
                    order = order,
                    onAddClick = onAddClick,
                    onOrderClick = { clicked ->
                        scope.launch {
                            programmatic = true
                            try {
                                listState.animateScrollToItem(index, -px)
                            } finally {
                                programmatic = false
                            }
                            if (index in orders.indices) {
                                lastCentered = index
                                onCenteredOrderChange(orders[index], index) // user-driven pin
                            }
                        }
                        onOrderClick(clicked)
                    },
                )
            }
        }
    }
}

@Composable
private fun orderCard(
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

            // ---- items count ----
            Text(
                text = "Items in order (${order.itemsCount})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.weight(1f))

            // ---- add orders button ----
            Button(
                onClick = { onAddClick(order) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_to_your_orders), maxLines = 1)
            }
        }
    }
}

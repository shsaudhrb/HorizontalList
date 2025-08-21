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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Composable
fun customBottom(
    orders: List<OrderInfo>,
    modifier: Modifier = Modifier,
    onAddClick: (OrderInfo) -> Unit = {},
    onOrderClick: (OrderInfo) -> Unit = {},
    onCenteredOrderChange: (order: OrderInfo, index: Int) -> Unit = { _, _ -> },
) {
    // state for scrolling
    val listState = rememberLazyListState()

    // for animating scroll-to-center when a card is tapped
    val scope = rememberCoroutineScope()

    // gets the device's width in dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // calculates how much empty space is left on the sides when one card is centered on the screen
    val sidePadding =
        ((screenWidth - dimensionResource(id = R.dimen.orders_card_width)) / 2).coerceAtLeast(0.dp)

    val startPadding = 16.dp

    // ensure the lazyrow cards always stop centered on screen when scrolling
    val cardBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Track which card index is currently centered in the lazy row
    var lastCenteredIndex by remember { mutableIntStateOf(-1) }

    // launched effect for centered the item when we scroll
    LaunchedEffect(orders, listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->

                // skip if there are no orders
                if (orders.isEmpty() || layoutInfo.visibleItemsInfo.isEmpty()) return@collect

                // calculate the horizontal center of the viewport ( middle of the screen )
                val viewportCenter =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2

                // find the visible item whose center is closest to the viewport center
                val nearest =
                    layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        val itemCenter = item.offset + item.size / 2
                        abs(itemCenter - viewportCenter)
                    }

                // if the centered item changed, notify via callback
                val newIndex = nearest?.index ?: return@collect
                if (newIndex != lastCenteredIndex && newIndex in orders.indices) {
                    lastCenteredIndex = newIndex
                    onCenteredOrderChange(orders[newIndex], newIndex)
                }
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.primary),
    ) {
        // ---- list of orders ----
        LazyRow(
            state = listState,
            flingBehavior = cardBehavior,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding =
                PaddingValues(
                    start = startPadding,
                    end = sidePadding,
                ),
        ) {
            itemsIndexed(
                items = orders,
                key = { _, item -> item.orderNumber },
            ) { index, order ->

                val densityPx = LocalDensity.current
                val sidePaddingPx = with(densityPx) { sidePadding.roundToPx() }

                orderCard(
                    order = order,
                    onAddClick = onAddClick,
                    onOrderClick = { clicked ->
                        // when a card is tapped: animate it into the center
                        scope.launch {
                            listState.animateScrollToItem(
                                index = index,
                                scrollOffset = -sidePaddingPx,
                            ) // negative to pull it to center
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
        onClick = { onOrderClick(order) }, // click on selected order
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                // ---- distance of orders ----
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
                    )
                    Text(
                        text = "${order.orderNumber} - ${order.timeAgo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- items count ----
            Text(
                text = "Items in order (${order.itemsCount})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.weight(1f))

            // ---- add orders button ----
            Button(
                onClick = { onAddClick(order) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("+ Add to your orders")
            }
        }
    }
}

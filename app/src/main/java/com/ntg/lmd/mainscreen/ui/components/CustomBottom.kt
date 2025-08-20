package com.ntg.lmd.mainscreen.ui.components

import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun customBottom(
    orders: List<OrderInfo>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    onAddClick: (OrderInfo) -> Unit = {},
    onOrderClick: (OrderInfo) -> Unit = {},
    onCenteredOrderChange: (order: OrderInfo, index: Int) -> Unit = { _, _ -> },
) {
    // lazy row state for orders
    val listState = rememberLazyListState()

    // coroutine scope for smooth map animations when camera focus change
    val scope = rememberCoroutineScope()

    // Card size & spacing
    val cardWidth: Dp = 300.dp
    val itemSpacing: Dp = 12.dp

    // Symmetric side padding so a centered item aligns to the screen center
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val sidePadding = ((screenWidth - cardWidth) / 2).coerceAtLeast(0.dp)

    // ensure the lazyrow cards always stop centered on screen when scrolling
    val cardBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Track which card index is currently centered in the lazy row
    var lastCenteredIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(orders, listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                if (orders.isEmpty() || layoutInfo.visibleItemsInfo.isEmpty()) return@collect
                val viewportCenter =
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val nearest =
                    layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        val itemCenter = item.offset + item.size / 2
                        abs(itemCenter - viewportCenter)
                    }
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
                .height(250.dp)
                .background(backgroundColor),
    ) {
        // ---- list of orders ----
        LazyRow(
            state = listState,
            flingBehavior = cardBehavior,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            contentPadding = PaddingValues(horizontal = sidePadding),
        ) {
            itemsIndexed(
                items = orders,
                key = { _, item -> item.orderNumber },
            ) { index, order ->
                orderCard(
                    order = order,
                    onAddClick = onAddClick,
                    onOrderClick = { clicked ->
                        // Center the clicked item
                        scope.launch {
                            listState.animateScrollToItem(index = index, scrollOffset = 0)
                        }
                        onOrderClick(clicked)
                    },
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
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
        modifier = modifier.size(width = 300.dp, height = 180.dp),
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
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format("%.1fkm", order.distanceKm),
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

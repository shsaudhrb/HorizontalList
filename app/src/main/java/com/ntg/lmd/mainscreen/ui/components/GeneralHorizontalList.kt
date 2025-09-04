package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val NEAR_END_THRESHOLD = 3
@Composable
fun generalHorizontalList(
    orders: List<OrderInfo>,
    modifier: Modifier = Modifier,
    callbacks: HorizontalListCallbacks = HorizontalListCallbacks(),
    cardContent: @Composable (OrderInfo, Int) -> Unit,
) {
    if (orders.isEmpty()) return
    val listState = rememberLazyListState()

    observeCenteredItem(listState, orders, callbacks.onCenteredOrderChange)
    observeNearEnd(listState, orders, callbacks.onNearEnd)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.myOrders_card_height))
                .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(orders, key = { _, order -> order.orderNumber }) { index, order ->
                cardContent(order, index)
            }
        }
    }
}

data class HorizontalListCallbacks(
    val onCenteredOrderChange: (OrderInfo, Int) -> Unit = { _, _ -> },
    val onNearEnd: (Int) -> Unit = {},
)

@Composable
private fun observeCenteredItem(
    listState: androidx.compose.foundation.lazy.LazyListState,
    orders: List<OrderInfo>,
    onCenteredOrderChange: (OrderInfo, Int) -> Unit,
) {
    LaunchedEffect(listState, orders) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { idx -> idx.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in orders.indices) {
                    onCenteredOrderChange(orders[idx], idx)
                }
            }
    }
}

@Composable
private fun observeNearEnd(
    listState: androidx.compose.foundation.lazy.LazyListState,
    orders: List<OrderInfo>,
    onNearEnd: (Int) -> Unit,
) {
    LaunchedEffect(listState, orders) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= orders.size - NEAR_END_THRESHOLD) {
                    onNearEnd(lastVisible)
                }
            }
    }
}

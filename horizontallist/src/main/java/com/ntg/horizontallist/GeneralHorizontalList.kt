package com.ntg.horizontallist

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
private const val NEAR_END_THRESHOLD = 3
@Composable
fun <T> GeneralHorizontalList(
    items: List<T>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    callbacks: GeneralHorizontalListCallbacks<T> = GeneralHorizontalListCallbacks(),
    key: (T) -> Any,
    cardContent: @Composable (T, Int) -> Unit,
) {
    if (items.isEmpty()) return
    val listState = rememberLazyListState()

    observeCenteredItem(listState, items, callbacks.onCenteredItemChange)
    observeNearEnd(listState, items, callbacks.onNearEnd)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                cardContent(item, index)
            }
        }
    }
}

data class GeneralHorizontalListCallbacks<T>(
    val onCenteredItemChange: (T, Int) -> Unit = { _, _ -> },
    val onNearEnd: (Int) -> Unit = {}
)

@Composable
private fun <T> observeCenteredItem(
    listState: androidx.compose.foundation.lazy.LazyListState,
    items: List<T>,
    onCenteredItemChange: (T, Int) -> Unit,
) {
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { idx -> idx.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in items.indices) {
                    onCenteredItemChange(items[idx], idx)
                }
            }
    }
}

@Composable
private fun <T> observeNearEnd(
    listState: androidx.compose.foundation.lazy.LazyListState,
    items: List<T>,
    onNearEnd: (Int) -> Unit,
) {
    LaunchedEffect(listState, items) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= items.size - NEAR_END_THRESHOLD) {
                    onNearEnd(lastVisible)
                }
            }
    }
}
package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun observeNearEnd(
    listState: LazyListState,
    isLoadingMore: Boolean,
    endReached: Boolean,
    onLoadMore: () -> Unit,
) {
    val loading by rememberUpdatedState(isLoadingMore)
    val ended by rememberUpdatedState(endReached)
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index
            val total = listState.layoutInfo.totalItemsCount
            last != null && total > 0 && last >= total - 2
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd && !ended && !loading) loadMore()
        }
    }
}

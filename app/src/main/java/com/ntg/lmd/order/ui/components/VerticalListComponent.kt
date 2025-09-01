package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.ntg.lmd.R
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> verticalListComponent(
    items: List<T>,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isLoadingMore: Boolean,
    endReached: Boolean,
    onLoadMore: () -> Unit,

    // Layout
    contentPadding: PaddingValues = PaddingValues(
        start = dimensionResource(R.dimen.mediumSpace),
        end = dimensionResource(R.dimen.mediumSpace),
        bottom = dimensionResource(R.dimen.mediumSpace),
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(
        dimensionResource(R.dimen.list_item_spacing)
    ),

    // Behavior
    prefetchThreshold: Int = 3,

    emptyContent: @Composable () -> Unit = { customEmptyState() },
    loadingFooter: @Composable () -> Unit = { customLoadingFooter() },
    endFooter: @Composable () -> Unit = { customEndFooter() },
) {
    // Initial loading
    val showingInitialLoading = (isRefreshing || isLoadingMore) && items.isEmpty()

    // Observe near-end to trigger pagination
    LaunchedEffect(listState, items.size, isLoadingMore, endReached, isRefreshing) {
        if (endReached) return@LaunchedEffect
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            last to total
        }.distinctUntilChanged().collect { (last, total) ->
            if (!isRefreshing && !isLoadingMore && !endReached && total > 0 &&
                last >= total - 1 - prefetchThreshold
            ) {
                onLoadMore()
            }
        }
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        when {
            showingInitialLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            items.isEmpty() -> {
                emptyContent()
            }

            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { key(it) }) { item ->
                        itemContent(item)
                    }
                    if (isLoadingMore) item("loading_footer") { loadingFooter() }
                    if (endReached && items.isNotEmpty()) item("end_footer") { endFooter() }
                }
            }
        }
    }
}

@Composable
fun customLoadingFooter() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.smallerSpace)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun customEndFooter() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.smallSpace)),
        contentAlignment = Alignment.Center,
    ) {
        Text("• End of list •", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun customEmptyState(
    text: String = "No items yet",
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

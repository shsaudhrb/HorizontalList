package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.VerticalListConfig
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> verticalListComponent(
    items: List<T>,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    config: VerticalListConfig,
) {
    // Initial loading
    val showingInitialLoading = (config.isRefreshing || config.isLoadingMore) && items.isEmpty()

    // Avoid multiple onLoadMore() calls while Compose/state is in-flight
    var loadRequested by remember { mutableStateOf(false) }

    // Reset the local guard when VM acknowledges loading or when list grows or we reach the end
    LaunchedEffect(config.isLoadingMore, items.size, config.endReached) {
        if (config.isLoadingMore || config.endReached) loadRequested = false
    }

    // load more when scrolled near the end
    paginateOnNearEnd(
        listState = config.listState,
        prefetch = config.prefetchThreshold,
        blockLoad = config.isRefreshing || config.isLoadingMore || config.endReached || loadRequested,
        onTrigger = {
            loadRequested = true
            config.onLoadMore()
        },
    )

    PullToRefreshBox(isRefreshing = config.isRefreshing, onRefresh = config.onRefresh) {
        when {
            showingInitialLoading -> loadingScreen()
            items.isEmpty() -> config.emptyContent()
            else -> listContent(items, key, itemContent, config)
        }
    }
}

@Composable
private fun loadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun <T> listContent(
    items: List<T>,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    config: VerticalListConfig,
) {
    LazyColumn(
        state = config.listState,
        contentPadding = config.contentPadding,
        verticalArrangement = config.verticalArrangement,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { key(it) }) { item -> itemContent(item) }
        if (config.isLoadingMore) item("loading_footer") { config.loadingFooter() }
        val showEnd = config.endReached && items.isNotEmpty() && !config.isLoadingMore
        if (showEnd) item(key = "end_footer_${items.size}_${config.endReached}") { config.endFooter() }
    }
}

@Composable
private fun paginateOnNearEnd(
    listState: LazyListState,
    prefetch: Int,
    blockLoad: Boolean,
    onTrigger: () -> Unit,
) {
    LaunchedEffect(listState, prefetch, blockLoad) {
        snapshotFlow {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            last to total
        }.distinctUntilChanged().collectLatest { (last, total) ->
            val nearEnd = total > 0 && last >= total - 1 - prefetch
            if (!blockLoad && nearEnd) onTrigger()
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
fun customEmptyState(text: String = "No items yet") {
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

package com.ntg.lmd.order.domain.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import com.ntg.lmd.R
import com.ntg.lmd.order.ui.components.customEmptyState
import com.ntg.lmd.order.ui.components.customEndFooter
import com.ntg.lmd.order.ui.components.customLoadingFooter

data class VerticalListConfig(
    val listState: LazyListState,
    val isRefreshing: Boolean,
    val onRefresh: () -> Unit,
    val isLoadingMore: Boolean,
    val endReached: Boolean,
    val onLoadMore: () -> Unit,
    val prefetchThreshold: Int = 3,
    val contentPadding: PaddingValues,
    val verticalArrangement: Arrangement.Vertical,
    val emptyContent: @Composable () -> Unit,
    val loadingFooter: @Composable () -> Unit,
    val endFooter: @Composable () -> Unit,
)

@Composable
fun defaultVerticalListConfig(
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isLoadingMore: Boolean,
    endReached: Boolean,
    onLoadMore: () -> Unit,
): VerticalListConfig =
    VerticalListConfig(
        listState = listState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        isLoadingMore = isLoadingMore,
        endReached = endReached,
        onLoadMore = onLoadMore,
        prefetchThreshold = 3,
        contentPadding =
            PaddingValues(
                start = dimensionResource(R.dimen.mediumSpace),
                end = dimensionResource(R.dimen.mediumSpace),
                bottom = dimensionResource(R.dimen.mediumSpace),
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.mediumSpace),
            ),
        emptyContent = { customEmptyState() },
        loadingFooter = { customLoadingFooter() },
        endFooter = { customEndFooter() },
    )

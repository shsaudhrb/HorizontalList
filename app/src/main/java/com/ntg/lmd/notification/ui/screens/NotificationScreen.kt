package com.ntg.lmd.notification.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ntg.lmd.R
import com.ntg.lmd.notification.data.model.FCMServiceLocator
import com.ntg.lmd.notification.data.repository.seedNotifications
import com.ntg.lmd.notification.domain.model.NotificationFilter
import com.ntg.lmd.notification.ui.components.emptySection
import com.ntg.lmd.notification.ui.components.errorSection
import com.ntg.lmd.notification.ui.components.filterRow
import com.ntg.lmd.notification.ui.components.footerAppendState
import com.ntg.lmd.notification.ui.components.notificationCard
import com.ntg.lmd.notification.ui.components.notificationPlaceholder
import com.ntg.lmd.notification.ui.model.NotificationUi
import com.ntg.lmd.notification.ui.viewmodel.NotificationsVMFactory
import com.ntg.lmd.notification.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun notificationScreen(
    viewModel: NotificationsViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel(factory = NotificationsVMFactory()),
) {
    val filter by viewModel.filter.collectAsState()
    val pagingFlow = viewModel.pagingDataFlow
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

    // Seed once
    seedNotificationsOnce()

    val isRefreshing =
        lazyPagingItems.loadState.refresh is LoadState.Loading &&
            lazyPagingItems.itemCount > 0

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            viewModel.addDummyAndRefresh()
            lazyPagingItems.refresh()
        },
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.smallSpace)),
    ) {
        notificationContent(
            lazyPagingItems = lazyPagingItems,
            filter = filter,
            onFilterChange = { viewModel.setFilter(it) },
        )
    }
}

@Composable
private fun notificationContent(
    lazyPagingItems: LazyPagingItems<NotificationUi>,
    filter: NotificationFilter,
    onFilterChange: (NotificationFilter) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        filterRow(filter = filter, onFilterChange = onFilterChange)
        Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))
        notificationBody(lazyPagingItems)
    }
}

@Composable
private fun ColumnScope.notificationBody(lazyPagingItems: LazyPagingItems<NotificationUi>) {
    when {
        lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
            centeredBox { CircularProgressIndicator() }
        }
        lazyPagingItems.loadState.refresh is LoadState.Error -> {
            val e = (lazyPagingItems.loadState.refresh as LoadState.Error).error
            centeredBox {
                errorSection(
                    message = e.message ?: stringResource(R.string.unable_load_notifications),
                    onRetry = { lazyPagingItems.retry() },
                )
            }
        }
        lazyPagingItems.itemCount == 0 -> {
            centeredBox { emptySection() }
        }
        else -> {
            notificationList(
                lazyPagingItems = lazyPagingItems,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColumnScope.centeredBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun notificationList(
    lazyPagingItems: LazyPagingItems<NotificationUi>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.smallSpace)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallSpace)),
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = { index -> lazyPagingItems[index]?.id ?: index.toLong() },
        ) { index ->
            lazyPagingItems[index]?.let {
                notificationCard(it)
            } ?: notificationPlaceholder()
        }

        footerAppendState(lazyPagingItems)
    }
}

// Debug only
@Composable
private fun seedNotificationsOnce() {
    val seeded = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!seeded.value) {
            seedNotifications(FCMServiceLocator.saveIncomingNotificationUseCase)
            seeded.value = true
        }
    }
}

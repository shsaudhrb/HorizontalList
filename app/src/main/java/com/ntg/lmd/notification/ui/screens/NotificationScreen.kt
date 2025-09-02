package com.ntg.lmd.notification.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ntg.lmd.R
import com.ntg.lmd.notification.data.dataSource.remote.ServiceLocator
import com.ntg.lmd.notification.data.repository.seedNotifications
import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.ui.components.filterRow
import com.ntg.lmd.notification.ui.model.NotificationUi
import com.ntg.lmd.notification.ui.model.relativeAgeLabel
import com.ntg.lmd.notification.ui.viewmodel.NotificationsVMFactory
import com.ntg.lmd.notification.ui.viewmodel.NotificationsViewModel
import com.ntg.lmd.order.ui.components.verticalListComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MILLIS_PER_MINUTE = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun notificationScreen(
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsVMFactory()),
) {
    val filter by viewModel.filter.collectAsState()
    val lazyPagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()

    // Seed once
    seedNotificationsOnce()

    val isRefreshing  = lazyPagingItems.loadState.refresh is LoadState.Loading
    val isLoadingMore = lazyPagingItems.loadState.append  is LoadState.Loading
    val endReached    = (lazyPagingItems.loadState.append as? LoadState.NotLoading)
        ?.endOfPaginationReached == true
    val items: List<NotificationUi> = remember(
        lazyPagingItems.itemCount, lazyPagingItems.loadState
    ) { List(lazyPagingItems.itemCount) { i -> lazyPagingItems[i] }.filterNotNull() }
    val topId = items.firstOrNull()?.id ?: -1L
    val listState = remember(topId) { LazyListState(0, 0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) listState.scrollToItem(0)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.smallSpace)),
    ) {
        filterRow(filter = filter, onFilterChange = { viewModel.setFilter(it) })
        Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))

        Box(Modifier.weight(1f)) {
            verticalListComponent(
                items = items,
                key = { item -> "$topId:${item.id}" },
                itemContent = { notificationCard(it) },
                listState = listState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch { listState.scrollToItem(0) }
                    viewModel.addDummyAndRefresh()
                    lazyPagingItems.refresh()
                },
                isLoadingMore = isLoadingMore,
                endReached = endReached,
                onLoadMore = {
                    if (lazyPagingItems.loadState.append !is LoadState.Loading && !endReached) {
                        val last = lazyPagingItems.itemCount - 1
                        if (last >= 0) {
                            @Suppress("UNUSED_EXPRESSION")
                            lazyPagingItems[last]
                        }
                    }
                },
                emptyContent = { emptySection() },
            )
        }
    }
}

@Composable
private fun notificationCard(item: NotificationUi) {
    val nowMs = rememberNowMillis(MILLIS_PER_MINUTE)
    val ageLabel = relativeAgeLabel(nowMs, item.timestampMs)

    val accent =
        when (item.type) {
            AgentNotification.Type.ORDER_STATUS -> MaterialTheme.colorScheme.primary
            AgentNotification.Type.WALLET -> MaterialTheme.colorScheme.tertiary
            AgentNotification.Type.OTHER -> MaterialTheme.colorScheme.secondary
        }
    val icon =
        when (item.type) {
            AgentNotification.Type.ORDER_STATUS -> Icons.Outlined.LocalShipping
            AgentNotification.Type.WALLET -> Icons.Outlined.AttachMoney
            AgentNotification.Type.OTHER -> Icons.Outlined.Notifications
        }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.cardRoundCorner)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimensionResource(R.dimen.smallElevation)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(dimensionResource(R.dimen.smallSpace)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(dimensionResource(R.dimen.notificationIconBox))
                        .background(accent.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }

            Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.tinySpace)))
                Text(
                    text = ageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(
                Modifier
                    .width(dimensionResource(R.dimen.extraSmallSpace))
                    .height(IntrinsicSize.Max)
                    .background(
                        accent.copy(alpha = 0.8f),
                        RoundedCornerShape(
                            topStart = dimensionResource(R.dimen.notificationAccentBarRadius),
                            bottomStart = dimensionResource(R.dimen.notificationAccentBarRadius),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun seedNotificationsOnce() {
    val seeded = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!seeded.value) {
            seedNotifications(ServiceLocator.saveIncomingNotificationUseCase)
//            seedNotifications(FCMServiceLocator.saveIncomingNotificationUseCase)
            seeded.value = true
        }
    }
}

private fun LazyListScope.footerAppendState(lazyPagingItems: LazyPagingItems<NotificationUi>) {
    when (val state = lazyPagingItems.loadState.append) {
        is LoadState.Loading -> {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.mediumSpace)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }

        is LoadState.Error -> {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.mediumSpace)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.error.message ?: stringResource(R.string.loading_more_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
                    Button(onClick = { lazyPagingItems.retry() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }

        is LoadState.NotLoading -> {
            if (state.endOfPaginationReached) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.largerSpace)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.caught_up),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun errorSection(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun emptySection() {
    val iconSize = dimensionResource(R.dimen.appLogoSize)

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
        Text(
            stringResource(R.string.empty_notifications),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@SuppressLint("AutoboxingStateCreation")
@Composable
private fun rememberNowMillis(tickMillis: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tickMillis) {
        while (true) {
            delay(tickMillis)
            now = System.currentTimeMillis()
        }
    }
    return now
}

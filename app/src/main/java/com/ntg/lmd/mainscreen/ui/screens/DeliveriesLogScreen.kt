package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.model.DeliveryState
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge

private const val ICON_COL_WEIGHT = 1f
private const val DETAILS_COL_WEIGHT = 3f
private const val TIME_COL_WEIGHT = 2f

@Composable
@Suppress("UNUSED_PARAMETER")
fun deliveriesLogScreen(
    navController: NavController,
    vm: DeliveriesLogViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.load(context) }

    val backStackEntry = navController.currentBackStackEntry

    val logs by vm.logs.collectAsState()
    val isLoadingMore by vm.isLoadingMore.collectAsState()
    val endReached by vm.endReached.collectAsState()

    LaunchedEffect(backStackEntry) {
        val h = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        val searchingFlow = h.getStateFlow("searching", false)
        val textFlow = h.getStateFlow("search_text", "")
        combine(
            searchingFlow,
            textFlow,
        ) { enabled, text -> if (enabled) text else "" } // when search is closed, reset
            .distinctUntilChanged()
            .collect { query -> vm.searchById(query) }
    }

    LaunchedEffect(backStackEntry) {
        val h = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("searching", false).collect { enabled ->
            if (!enabled) vm.searchById("") // restore full list
        }
    }

    LaunchedEffect(backStackEntry) {
        val h = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("search_submit", "").collect { submitted ->
            if (submitted.isNotEmpty()) vm.searchById(submitted)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header Row
        headerRow()

        HorizontalDivider()

        // Logs
        logsList(
            logs = logs,
            isLoadingMore = isLoadingMore,
            endReached = endReached,
            onLoadMore = { vm.loadMore() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun headerRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(ICON_COL_WEIGHT), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.SLA),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(R.string.order_details),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(DETAILS_COL_WEIGHT),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.delivery_time),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(TIME_COL_WEIGHT),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun logsList(
    logs: List<DeliveryLog>,
    isLoadingMore: Boolean,
    endReached: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val loading by rememberUpdatedState(isLoadingMore)
    val ended by rememberUpdatedState(endReached)
    val loadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        // load until list becomes scrollable
        val notScrollableFlow =
            snapshotFlow { !listState.canScrollForward }
                .distinctUntilChanged()

        // load when user nears the end
        val nearEndFlow =
            snapshotFlow {
                val last =
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index
                val total = listState.layoutInfo.totalItemsCount
                last != null && total > 0 && last >= total - 2
            }.distinctUntilChanged()

        merge(notScrollableFlow, nearEndFlow).collect { shouldLoad ->
            if (shouldLoad && !ended && !loading) {
                loadMore()
            }
        }
    }

    if (logs.isEmpty() && !isLoadingMore) {
        // empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.no_deliveries_yet),
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(logs) { log -> deliveryLogItem(log) }
        if (isLoadingMore && !endReached) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun deliveryLogItem(log: DeliveryLog) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(ICON_COL_WEIGHT), contentAlignment = Alignment.Center) {
                when (log.state) {
                    DeliveryState.DELIVERED ->
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen)

                    DeliveryState.CANCELLED, DeliveryState.FAILED ->
                        Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.error)

                    else ->
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
            Column(
                Modifier.weight(DETAILS_COL_WEIGHT),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    log.orderDate,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    log.orderId,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = log.deliveryTime,
                color =
                    when (log.state) {
                        DeliveryState.DELIVERED -> SuccessGreen
                        DeliveryState.CANCELLED, DeliveryState.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.weight(TIME_COL_WEIGHT),
                textAlign = TextAlign.Center,
            )
        }
    }
}

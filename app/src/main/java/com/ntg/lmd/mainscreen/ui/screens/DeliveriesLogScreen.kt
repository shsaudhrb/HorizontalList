package com.ntg.lmd.mainscreen.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.LogsUi
import com.ntg.lmd.mainscreen.ui.components.deliveryLogItem
import com.ntg.lmd.mainscreen.ui.components.emptyState
import com.ntg.lmd.mainscreen.ui.components.loadingFooter
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.order.domain.model.PagingState
import com.ntg.lmd.order.domain.model.VerticalListConfig
import com.ntg.lmd.order.domain.model.defaultVerticalListConfig
import com.ntg.lmd.order.ui.components.verticalListComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun deliveriesLogScreen(
    navController: NavController
) {
    val vm: DeliveriesLogViewModel = koinViewModel()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.load() }
    observeSearch(navController, vm)

    val ui = rememberLogsUi(vm)
    val bundle = rememberLogListBundle(ui, vm)

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        headerRow()
        HorizontalDivider()
        Box(Modifier.weight(1f)) {
            verticalListComponent(
                items = ui.logs,
                key = { it.orderId },
                itemContent = { deliveryLogItem(it) },
                config = bundle.config,
            )
        }
    }
}

@Composable
private fun rememberLogListBundle(
    ui: LogsUi,
    vm: DeliveriesLogViewModel,
): LogListBundle {
    val listState = rememberLazyListState()

    // Only remember the non-composable PagingState
    val paging =
        remember(ui.refreshing, ui.loadingMore, ui.endReached) {
            PagingState(
                isRefreshing = ui.refreshing,
                onRefresh = { vm.refresh() },
                isLoadingMore = ui.loadingMore,
                endReached = ui.endReached,
                onLoadMore = { vm.loadMore() },
            )
        }

    // Call the composable function outside of remember
    val config =
        defaultVerticalListConfig(
            listState = listState,
            paging = paging,
        ).copy(
            emptyContent = {
                if (!ui.refreshing && !ui.loadingMore) emptyState(Modifier.fillMaxSize())
            },
            loadingFooter = { loadingFooter() },
        )

    return LogListBundle(config)
}

private data class LogListBundle(
    val config: VerticalListConfig,
)

@Composable
private fun observeSearch(
    navController: NavController,
    vm: DeliveriesLogViewModel,
) {
    val back = navController.currentBackStackEntry
    LaunchedEffect(back) {
        val h = back?.savedStateHandle ?: return@LaunchedEffect
        combine(
            h.getStateFlow("searching", false),
            h.getStateFlow("search_text", ""),
        ) { en, t -> if (en) t else "" }
            .distinctUntilChanged()
            .collect { q ->
                vm.searchById(q)
                vm.load()
            }
    }
    LaunchedEffect(back) {
        val h = back?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("search_submit", "").collect { s ->
            if (s.isNotEmpty()) {
                vm.searchById(s)
                vm.load()
                h["search_submit"] = ""
            }
        }
    }
}

@Composable
private fun rememberLogsUi(vm: DeliveriesLogViewModel): LogsUi {
    val logs by vm.logs.collectAsState()
    val lm by vm.isLoadingMore.collectAsState()
    val er by vm.endReached.collectAsState()
    val rf by vm.isRefreshing.collectAsState()
    return LogsUi(logs, lm, er, rf)
}

@Composable
private fun headerRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        headerText(R.string.SLA)
        headerText(R.string.order_details)
        headerText(R.string.delivery_time)
    }
}

@Composable
private fun headerText(
    @StringRes textRes: Int,
) {
    Text(
        text = stringResource(textRes),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

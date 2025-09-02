package com.ntg.lmd.mainscreen.ui.screens

import android.content.Context
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
import com.ntg.lmd.mainscreen.domain.model.LogsUi
import com.ntg.lmd.mainscreen.ui.components.deliveryLogItem
import com.ntg.lmd.mainscreen.ui.components.emptyState
import com.ntg.lmd.mainscreen.ui.components.loadingFooter
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.order.domain.model.defaultVerticalListConfig
import com.ntg.lmd.order.ui.components.verticalListComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun deliveriesLogScreen(
    navController: NavController,
    vm: DeliveriesLogViewModel = viewModel(),
) {
    val ctx = LocalContext.current

    // initial load
    LaunchedEffect(Unit) { vm.load(ctx) }

    // keep your existing search wiring
    observeSearch(navController, vm, ctx)

    // collect UI state
    val ui = rememberLogsUi(vm)

    val listState = rememberLazyListState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        headerRow()
        HorizontalDivider()

        Box(Modifier.weight(1f)) {
            val config =
                defaultVerticalListConfig(
                    listState = listState,
                    isRefreshing = ui.refreshing,
                    onRefresh = { vm.refresh(ctx) },
                    isLoadingMore = ui.loadingMore,
                    endReached = ui.endReached,
                    onLoadMore = { vm.loadMore(ctx) },
                ).copy(
                    emptyContent = { emptyState(Modifier.fillMaxSize()) },
                    loadingFooter = { loadingFooter() },
                )
            verticalListComponent(
                items = ui.logs,
                key = { it.orderId },
                itemContent = { deliveryLogItem(it) },
                config = config,
            )
        }
    }
}

@Composable
private fun observeSearch(
    navController: NavController,
    vm: DeliveriesLogViewModel,
    ctx: Context,
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
                vm.load(ctx)
            }
    }
    LaunchedEffect(back) {
        val h = back?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("search_submit", "").collect { s ->
            if (s.isNotEmpty()) {
                vm.searchById(s)
                vm.load(ctx)
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
    @androidx.annotation.StringRes textRes: Int,
) {
    Text(
        text = stringResource(textRes),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

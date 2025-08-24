package com.ntg.lmd.order.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.OrdersDialogsCallbacks
import com.ntg.lmd.order.domain.model.OrdersDialogsState
import com.ntg.lmd.order.ui.components.exportOrdersHistoryPdf
import com.ntg.lmd.order.ui.components.orderHistoryCard
import com.ntg.lmd.order.ui.components.ordersHistoryDialogs
import com.ntg.lmd.order.ui.components.ordersHistoryMenu
import com.ntg.lmd.order.ui.components.sharePdf
import com.ntg.lmd.order.ui.viewmodel.OrderHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.isNotEmpty

@Composable
fun ordersHistoryRoute(registerOpenMenu: ((() -> Unit) -> Unit)? = null) {
    val vm: OrderHistoryViewModel = viewModel()
    val orders by vm.orders.collectAsState(emptyList())
    val filter by vm.filter.collectAsState()
    val isLoadingMore by vm.isLoadingMore.collectAsState()
    val endReached by vm.endReached.collectAsState()

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadFromAssets(ctx) }
    LaunchedEffect(Unit) { registerOpenMenu?.invoke { menuOpen = true } }
    LaunchedEffect(listState, orders) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: 0
        }.collect { lastVisible -> vm.loadMoreIfNeeded(lastVisible) }
    }

    ordersHistoryContent(
        orders = orders,
        isLoadingMore = isLoadingMore,
        endReached = endReached,
        listState = listState,
        ctx = ctx,
    )

    ordersHistoryDialogs(
        state =
            OrdersDialogsState(
                showFilter = showFilterDialog,
                showSort = showSortDialog,
                filter = filter,
            ),
        callbacks =
            OrdersDialogsCallbacks(
                onFilterDismiss = { showFilterDialog = false },
                onSortDismiss = { showSortDialog = false },
                onApplyFilter = { allowed -> vm.setAllowedStatuses(allowed) },
                onApplySort = { asc -> vm.setAgeAscending(asc) },
            ),
    )

    ordersHistoryMenu(
        open = menuOpen,
        onClose = { menuOpen = false },
        onFilterClick = { showFilterDialog = true },
        onSortClick = { showSortDialog = true },
        onExportPdfClick = {
            scope.launch(Dispatchers.IO) {
                val uri = exportOrdersHistoryPdf(ctx, orders)
                withContext(Dispatchers.Main) { uri?.let { sharePdf(ctx, it) } }
            }
        },
    )
}

@Composable
fun ordersHistoryContent(
    orders: List<OrderHistoryUi>,
    isLoadingMore: Boolean,
    endReached: Boolean,
    listState: LazyListState,
    ctx: Context,
) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_spacing)),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.text_spacing_medium)),
    ) {
        items(orders, key = { it.number }) {
            orderHistoryCard(ctx, order = it)
        }

        if (isLoadingMore) {
            item("loading_footer") {
                Row(
                    Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.text_spacing_small)),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            }
        }

        if (endReached && orders.isNotEmpty()) {
            item("end_footer") {
                Box(
                    Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.text_spacing_small)),
                    contentAlignment = Alignment.Center,
                ) { Text("• End of list •") }
            }
        }
    }
}

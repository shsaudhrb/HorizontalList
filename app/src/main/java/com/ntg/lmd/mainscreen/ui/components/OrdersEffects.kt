package com.ntg.lmd.mainscreen.ui.components

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.ui.components.OrdersUiConstants.VISIBLE_THRESHOLD
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.model.MyOrdersUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ordersEffects(
    vm: MyOrdersViewModel,
    state: MyOrdersUiState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    context: Context,
) {
    loadInitialOrdersEffect(vm, context)
    collectStatusUpdatesEffect(vm)
    snackbarErrorEffect(snackbarHostState, context)
    loadMoreEffect(vm, state, listState, context)
}

@Composable
private fun loadInitialOrdersEffect(
    vm: MyOrdersViewModel,
    context: Context,
) {
    LaunchedEffect(Unit) {
        vm.loadOrders(context)
    }
}

@Composable
private fun collectStatusUpdatesEffect(vm: MyOrdersViewModel) {
    LaunchedEffect(Unit) {
        LocalUiOnlyStatusBus.statusEvents.collectLatest { (id, newStatus) ->
            vm.updateStatusLocally(id, newStatus)
        }
    }
}

@Composable
private fun snackbarErrorEffect(
    snackbarHostState: SnackbarHostState,
    context: Context,
) {
    LaunchedEffect(Unit) {
        LocalUiOnlyStatusBus.errorEvents.collectLatest { (msg, retry) ->
            val result =
                snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = retry?.let { context.getString(R.string.retry) },
                    withDismissAction = true,
                )
            if (result == SnackbarResult.ActionPerformed) retry?.invoke()
        }
    }
}

@Composable
private fun loadMoreEffect(
    vm: MyOrdersViewModel,
    state: MyOrdersUiState,
    listState: LazyListState,
    context: Context,
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - VISIBLE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore, state.isRefreshing, state.isLoading) {
        if (shouldLoadMore && !state.isRefreshing && !state.isLoading) {
            vm.loadNextPage(context)
        }
    }
}

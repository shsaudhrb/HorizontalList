package com.ntg.lmd.mainscreen.ui.components

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.components.OrdersUiConstants.VISIBLE_THRESHOLD
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ordersEffects(
    vm: MyOrdersViewModel,
    updateVm: UpdateOrderStatusViewModel,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    context: Context,
) {
    val uiState by vm.uiState.collectAsState()
    ordersInitialLoadEffect(vm, context)
    localStatusBusEffect(vm)
    updateSuccessEffect(updateVm, vm)
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

    val shouldLoadMore by remember {
        derivedStateOf {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - VISIBLE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore, uiState.isRefreshing, uiState.isLoading) {
        if (shouldLoadMore && !uiState.isRefreshing && !uiState.isLoading) {
            vm.listVM.loadNextPage(context)
        }
    }
}

@Composable
private fun ordersInitialLoadEffect(
    vm: MyOrdersViewModel,
    context: Context,
) {
    LaunchedEffect(Unit) { vm.listVM.loadOrders(context) }
}

@Composable
private fun localStatusBusEffect(vm: MyOrdersViewModel) {
    LaunchedEffect(Unit) {
        LocalUiOnlyStatusBus.statusEvents.collectLatest { (id, newStatus) ->
            vm.statusVM.updateStatusLocally(id, newStatus)
        }
    }
}

@Composable
private fun updateSuccessEffect(
    updateVm: UpdateOrderStatusViewModel,
    vm: MyOrdersViewModel,
) {
    LaunchedEffect(Unit) {
        updateVm.success.collectLatest { serverOrder: OrderInfo ->
            vm.statusVM.applyServerPatch(serverOrder)
            vm.listVM.refreshOrders()
        }
    }
}

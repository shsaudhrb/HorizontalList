package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.data.repository.MyOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepository
import com.ntg.lmd.mainscreen.data.repository.UsersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
import com.ntg.lmd.mainscreen.ui.components.ActionDialog
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.CARD_ELEVATION
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.DETAILS_BUTTON_WEIGHT
import com.ntg.lmd.mainscreen.ui.components.OrdersUi.OUTLINE_STROKE
import com.ntg.lmd.mainscreen.ui.components.bottomStickyButton
import com.ntg.lmd.mainscreen.ui.components.callButton
import com.ntg.lmd.mainscreen.ui.components.deliverDialog
import com.ntg.lmd.mainscreen.ui.components.orderHeaderWithMenu
import com.ntg.lmd.mainscreen.ui.components.ordersContent
import com.ntg.lmd.mainscreen.ui.components.ordersEffects
import com.ntg.lmd.mainscreen.ui.components.primaryActionButton
import com.ntg.lmd.mainscreen.ui.components.reasonDialog
import com.ntg.lmd.mainscreen.ui.components.reassignBottomSheet
import com.ntg.lmd.mainscreen.ui.components.reassignDialog
import com.ntg.lmd.mainscreen.ui.components.simpleConfirmDialog
import com.ntg.lmd.mainscreen.ui.screens.orders.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.ActiveAgentsViewModelFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.MyOrdersViewModelFactory
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel.OrderLogger
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModelFactory
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.utils.SecureUserStore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun myOrdersScreen(onOpenOrderDetails: (String) -> Unit) {
    val repo = remember { MyOrdersRepositoryImpl(RetrofitProvider.ordersApi) }
    val updaterepo = remember { UpdateOrdersStatusRepository(RetrofitProvider.updateStatusApi) }

    val getUsecase = remember { GetMyOrdersUseCase(repo) }
    val updateUsecase = remember { UpdateOrderStatusUseCase(updaterepo) }

    val ordersVm: MyOrdersViewModel = viewModel(factory = MyOrdersViewModelFactory(getUsecase))
    val context = LocalContext.current

    val userStore = remember { SecureUserStore(context) }
    val updateVm: UpdateOrderStatusViewModel =
        viewModel(
            factory =
                UpdateOrderStatusViewModelFactory(
                    updateUsecase,
                    userStore,
                ),
        )
    val repoUsers = remember { UsersRepositoryImpl(RetrofitProvider.usersApi) }
    val getUsers = remember { GetActiveUsersUseCase(repoUsers) }
    val agentsVm: ActiveAgentsViewModel = viewModel(factory = ActiveAgentsViewModelFactory(getUsers))
    val agentsState by agentsVm.state.collectAsState()

    var reassignOrderId by remember { mutableStateOf<String?>(null) }

    // --- pass a lambda to open sheet from card:
    val onReassignRequested: (String) -> Unit = { orderId ->
        reassignOrderId = orderId
        agentsVm.load()
    }
    val state by ordersVm.state.collectAsState()
    val updatingIds by updateVm.updatingIds.collectAsState()
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()

    ordersEffects(ordersVm, state, listState, snack, ctx)

    LaunchedEffect(Unit) {
        updateVm.success.collect { updated ->
            ordersVm.applyServerPatch(updated)
        }
    }
    LaunchedEffect(Unit) {
        updateVm.error.collect { (msg, retry) ->
            val res =
                snack.showSnackbar(
                    message = msg,
                    actionLabel = ctx.getString(R.string.retry),
                    withDismissAction = true,
                )
            if (res == SnackbarResult.ActionPerformed) retry()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = { bottomStickyButton(text = stringResource(R.string.order_pool)) {} },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { ordersVm.refresh(ctx) },
            state = pullState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ordersContent(
                ordersVm = ordersVm,
                updateVm = updateVm,
                state = state,
                listState = listState,
                onOpenOrderDetails = onOpenOrderDetails,
                context = ctx,
                updatingIds = updatingIds,
                onReassignRequested = onReassignRequested,
            )
        }
    }
    reassignBottomSheet(
        open = reassignOrderId != null,
        state = agentsState,
        onDismiss = { reassignOrderId = null },
        onRetry = { agentsVm.load() },
        onSelect = { user ->
            val orderId = reassignOrderId ?: return@reassignBottomSheet
            OrderLogger.uiTap(orderId,
                state.orders.firstOrNull
                { it.id == orderId }?.orderNumber, "Menu:Reassign→${user.name}")
            updateVm.update(orderId, OrderStatus.REASSIGNED, assignedAgentId = user.id)
            reassignOrderId = null
        },
    )
}





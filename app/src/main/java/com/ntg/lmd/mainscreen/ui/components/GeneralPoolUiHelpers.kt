package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.model.GeneralPoolUiState
import com.ntg.lmd.mainscreen.ui.viewmodel.GeneralPoolViewModel

@Composable
fun poolBottomContent(
    ui: GeneralPoolUiState,
    viewModel: GeneralPoolViewModel,
    focusOnOrder: (OrderInfo, Boolean) -> Unit,
) {
    when {
        ui.isLoading -> loadingText()
        ui.mapOrders.isNotEmpty() -> ordersHorizontalList(ui, viewModel, focusOnOrder)
    }
}

@Composable
fun loadingText() {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.loading_text),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun ordersHorizontalList(
    ui: GeneralPoolUiState,
    viewModel: GeneralPoolViewModel,
    focusOnOrder: (OrderInfo, Boolean) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.BottomCenter)) {
            generalHorizontalList(
                orders = ui.mapOrders,
                callbacks =
                    HorizontalListCallbacks(
                        onCenteredOrderChange = { order, _ ->
                            focusOnOrder(order, false)
                            viewModel.onOrderSelected(order)
                        },
                        onNearEnd = { idx ->
                            // viewModel.loadNextIfNeeded(idx)
                        },
                    ),
                cardContent = { order, _ ->
                    orderCard(
                        order = order,
                        onAddClick = { /* handle add */ },
                        onOrderClick = { clicked -> focusOnOrder(clicked, false) },
                    )
                },
            )
        }
    }
}

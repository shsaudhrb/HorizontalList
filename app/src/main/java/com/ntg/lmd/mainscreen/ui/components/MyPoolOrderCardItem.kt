package com.ntg.lmd.mainscreen.ui.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModel
import com.ntg.lmd.mainscreen.ui.viewmodel.UpdateOrderStatusViewModelFactory

@Composable
fun myPoolOrderCardItem(
    order: OrderInfo,
    onOpenOrderDetails: (String) -> Unit,
    onCall: (String?) -> Unit,
) {
    Box(Modifier.width(dimensionResource(R.dimen.myOrders_card_width))) {
        val updateVm: UpdateOrderStatusViewModel = viewModel(
            factory = UpdateOrderStatusViewModelFactory(LocalContext.current.applicationContext as Application)
        )

        myOrderCard(
            order = order,
            isUpdating = false,
            onDetails = { onOpenOrderDetails(order.orderNumber) },
            onCall = { onCall(order.customerPhone)},
            onAction = {},
            onReassignRequested = {},
            updateVm =updateVm ,
        )
    }
}

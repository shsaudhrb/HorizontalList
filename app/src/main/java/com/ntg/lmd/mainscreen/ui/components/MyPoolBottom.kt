package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.model.LocalUiOnlyStatusBus
import com.ntg.lmd.mainscreen.ui.screens.myOrderCard
import com.ntg.lmd.mainscreen.ui.viewmodel.NEAR_END_THRESHOLD
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
fun myPoolBottom(
    orders: List<OrderInfo>,
    onCenteredOrderChange: (OrderInfo, Int) -> Unit = { _, _ -> },
    onOpenOrderDetails: (String) -> Unit,
    onNearEnd: (Int) -> Unit = {},
) {
    val listState = rememberLazyListState()

    val sidePadding =
        ((LocalConfiguration.current.screenWidthDp.dp - dimensionResource(id = R.dimen.myOrders_card_width)) / 2)
            .coerceAtLeast(0.dp)

    var lastCentered by remember { mutableIntStateOf(-1) }
    var programmatic by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(orders, listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                -1
            } else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo
                    .minByOrNull {
                        abs((it.offset + it.size / 2) - center)
                    }?.index ?: -1
            }
        }.distinctUntilChanged()
            .collect { idx ->
                if (!programmatic && idx in orders.indices && idx != lastCentered) {
                    lastCentered = idx
                    onCenteredOrderChange(orders[idx], idx)
                }
            }
    }

    LaunchedEffect(orders, listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= 0 && orders.isNotEmpty()) {
                    val triggerIndex = (orders.size - NEAR_END_THRESHOLD).coerceAtLeast(0)
                    if (lastVisible >= triggerIndex) {
                        onNearEnd(lastVisible) // delegate to VM
                    }
                }
            }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.orders_carousel_height))
            .background(MaterialTheme.colorScheme.primary),
    ) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.largeSpace)),
            contentPadding = PaddingValues(start = sidePadding, end = sidePadding),
        ) {
            itemsIndexed(
                items = orders,
                key = { _, order -> order.orderNumber },
            ) { _, info ->
                Box(Modifier.width(dimensionResource(R.dimen.myOrders_card_width))) {
                    myOrderCard(
                        order = info,
                        onDetails = { onOpenOrderDetails(info.orderNumber) },
                        onConfirmOrPick = { },
                        onCall = {
                            val phone = info.customerPhone
                            if (!phone.isNullOrBlank()) {
                                val intent =
                                    android.content.Intent(
                                        android.content.Intent.ACTION_DIAL,
                                        "tel:$phone".toUri(),
                                    )
                                context.startActivity(intent)
                            } else {
                                LocalUiOnlyStatusBus.errorEvents.tryEmit(
                                    context.getString(R.string.phone_missing) to null,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.ui.model.BottomCallbacks
import com.ntg.lmd.mainscreen.ui.model.BottomDeps
import kotlinx.coroutines.launch

@Composable
fun ordersRow(
    deps: BottomDeps,
    setProgrammatic: (Boolean) -> Unit,
    onSnapAndCenter: (Int) -> Unit,
    callbacks: BottomCallbacks,
) {
    val scope = rememberCoroutineScope()

    LazyRow(
        state = deps.listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = deps.listState),
        modifier = Modifier.fillMaxWidth(), // <-- just this
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = deps.sidePadding, end = deps.sidePadding),
    ) {
        itemsIndexed(deps.orders, key = { _, o -> o.orderNumber }) { index, order ->
            orderCard(
                order = order,
                onAddClick = callbacks.onAddClick,
                onOrderClick = { clicked ->
                    scope.launch {
                        setProgrammatic(true)
                        try {
                            deps.listState.animateScrollToItem(index, -deps.px)
                        } finally {
                            setProgrammatic(false)
                        }
                        if (index in deps.orders.indices) onSnapAndCenter(index)
                    }
                    callbacks.onOrderClick(clicked)
                },
            )
        }
    }
}

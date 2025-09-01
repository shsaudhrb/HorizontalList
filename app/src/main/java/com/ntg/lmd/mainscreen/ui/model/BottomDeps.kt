package com.ntg.lmd.mainscreen.ui.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Dp
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class BottomDeps(
    val orders: List<OrderInfo>,
    val listState: LazyListState,
    val sidePadding: Dp,
    val px: Int,
)

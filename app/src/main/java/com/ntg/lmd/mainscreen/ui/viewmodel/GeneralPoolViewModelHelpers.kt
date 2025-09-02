package com.ntg.lmd.mainscreen.ui.viewmodel

import android.provider.SyncStateContract.Helpers.update
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.ui.model.GeneralPoolUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal fun mergeOrders(
    existing: List<OrderInfo>,
    incoming: List<OrderInfo>,
): List<OrderInfo> {
    if (existing.isEmpty()) return incoming
    if (incoming.isEmpty()) return existing

    val map = existing.associateBy { it.orderNumber }.toMutableMap()
    for (o in incoming) map[o.orderNumber] = o
    return map.values.toList()
}

internal fun determineNextSelection(
    merged: List<OrderInfo>,
    currentSel: OrderInfo?,
    userPinnedSelection: Boolean,
): OrderInfo? =
    when {
        userPinnedSelection -> currentSel
        currentSel == null -> merged.firstOrNull { it.lat != 0.0 && it.lng != 0.0 } ?: merged.firstOrNull()
        merged.none { it.orderNumber == currentSel.orderNumber } -> null
        else -> merged.firstOrNull { it.orderNumber == currentSel.orderNumber }
    }

internal fun pickDefaultSelection(
    current: OrderInfo?,
    initial: List<OrderInfo>,
): OrderInfo? =
    current ?: initial.firstOrNull {
        it.lat != 0.0 && it.lng != 0.0
    } ?: initial.firstOrNull()

internal fun MutableStateFlow<GeneralPoolUiState>.ensureSelectedStillVisible(update: GeneralPoolUiState.() -> GeneralPoolUiState) {
    this.update(update)
}

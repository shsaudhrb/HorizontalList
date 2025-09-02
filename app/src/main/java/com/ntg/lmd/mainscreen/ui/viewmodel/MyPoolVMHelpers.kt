package com.ntg.lmd.mainscreen.ui.viewmodel

import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrdersPage
import com.ntg.lmd.mainscreen.ui.model.MyPoolLoadResult
import java.util.LinkedHashMap
import kotlin.coroutines.cancellation.CancellationException

fun mergeById(
    existing: List<OrderInfo>,
    incoming: List<OrderInfo>,
): List<OrderInfo> {
    if (incoming.isEmpty()) return existing
    val map = LinkedHashMap<String, OrderInfo>(existing.size + incoming.size)
    existing.forEach { map[it.orderNumber] = it }
    incoming.forEach { map[it.orderNumber] = it }
    return map.values.toList()
}

/** Loop pages until we accumulate at least pageSize items or hit end. */
suspend fun fillPagesForInitial(
    pageSize: Int,
    fetch: suspend (page: Int, limit: Int) -> OrdersPage,
    acc: MutableList<OrderInfo>,
): Pair<Boolean, Int> {
    var curPage = 1
    var reachedEnd = false
    while (acc.size < pageSize && !reachedEnd) {
        val r = fetch(curPage, pageSize)
        if (r.items.isNotEmpty()) acc += r.items
        reachedEnd = r.rawCount < pageSize
        curPage++
    }
    return reachedEnd to (curPage - 1)
}
// PagingHelpers.kt (or at bottom of MyPoolViewModel.kt but OUTSIDE the class)

fun handleErrorPaging(ex: Throwable): MyPoolLoadResult =
    when (ex) {
        is CancellationException -> throw ex
        else -> MyPoolLoadResult.Error(ex)
    }

fun handleSuccessPaging(
    res: OrdersPage,
    pageAt: Int,
    pageSize: Int,
    append: (items: List<OrderInfo>, rawCount: Int, pageAt: Int) -> Unit,
): MyPoolLoadResult =
    when {
        res.items.isNotEmpty() -> {
            append(res.items, res.rawCount, pageAt)
            MyPoolLoadResult.Appended
        }
        res.rawCount < pageSize -> MyPoolLoadResult.EndReached(pageAt)
        else -> MyPoolLoadResult.NoChange(pageAt + 1)
    }

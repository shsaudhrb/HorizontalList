package com.ntg.lmd.mainscreen.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.ntg.lmd.mainscreen.data.datasource.remote.LiveOrdersApiService
import com.ntg.lmd.mainscreen.data.datasource.remote.PagedOrdersResponse
import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import com.ntg.lmd.network.sockets.SocketIntegration
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val ORDERS_TAG = "OrdersRepo"

class OrdersRepositoryImpl(
    private val liveOrdersApi: LiveOrdersApiService,
    private val socket: SocketIntegration,
) : OrdersRepository {
    override suspend fun getAllLiveOrders(pageSize: Int): Result<List<Order>> =
        try {
            val all = mutableListOf<Order>()
            var page = 1
            var hop = 0

            Log.d(ORDERS_TAG, "=== PAGED FETCH START (pageSize=$pageSize) ===")

            while (true) {
                hop += 1
                Log.d(ORDERS_TAG, "→ hop#$hop request: page=$page, limit=$pageSize")

                val resp = safeFetchPage(liveOrdersApi, page, pageSize) // throws if !success
                val (items, pi) = extractOrdersAndPageInfo(resp.data)
                all += items

                val (currentPage, totalPages, hintedNext) = resolvePageMeta(resp, page, pi)
                logHopResponse(hop, items.size, currentPage, totalPages, hintedNext)

                val next = computeNextPage(currentPage, totalPages, hintedNext)
                if (items.isEmpty() || next == null) {
                    Log.d(ORDERS_TAG, "=== PAGED FETCH END (hops=$hop, totalItems=${all.size}) ===")
                    break
                }

                page = next
            }

            Result.success(all)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.e(ORDERS_TAG, "HTTP ${e.code()}: ${e.message()}", e)
            Result.failure(IllegalStateException("HTTP ${e.code()}: ${e.message()}", e))
        } catch (e: IOException) {
            Log.e(ORDERS_TAG, "Network error: ${e.message}", e)
            Result.failure(IllegalStateException("Network error: ${e.message}", e))
        } catch (e: JsonSyntaxException) {
            Log.e(ORDERS_TAG, "JSON syntax error: ${e.message}", e)
            Result.failure(IllegalStateException("JSON syntax error: ${e.message}", e))
        } catch (e: JsonParseException) {
            Log.e(ORDERS_TAG, "JSON parse error: ${e.message}", e)
            Result.failure(IllegalStateException("JSON parse error: ${e.message}", e))
        } catch (e: IllegalStateException) {
            Log.e(ORDERS_TAG, "Unexpected JSON structure: ${e.message}", e)
            Result.failure(IllegalStateException("Unexpected JSON structure: ${e.message}", e))
        }

    override fun connectToOrders(channelName: String) {
        socket.connect(channelName)
        socket.startChannelListener()
    }

    override fun disconnectFromOrders() {
        socket.disconnect()
    }

    override fun retryConnection() {
        socket.retryConnection()
    }

    override fun updateOrderStatus(
        orderId: String,
        status: String,
    ) {
        socket.updateOrderStatus(orderId, status)
    }

    fun connectionState() = socket.connectionState

    fun orders() = socket.orders

    fun orderChannel() = socket.getOrderChannel()
}

private data class PageInfo(
    val page: Int? = null,
    val totalPages: Int? = null,
    val nextPage: Int? = null,
    val hasMore: Boolean? = null,
    val cursor: String? = null,
    val nextCursor: String? = null,
)

private val PREFERRED_ARRAY_KEYS =
    listOf(
        "items",
        "orders",
        "initialOrders",
        "results",
        "rows",
        "list",
        "liveOrders",
        "data",
    )

private suspend fun safeFetchPage(
    api: LiveOrdersApiService,
    page: Int,
    limit: Int,
): PagedOrdersResponse {
    val resp = api.getLiveOrdersPage(page = page, limit = limit, search = null)
    if (!resp.success) {
        throw IllegalStateException(resp.message ?: "Failed to load orders")
    }
    return resp
}

private fun resolvePageMeta(
    resp: PagedOrdersResponse,
    fallbackPage: Int,
    pi: PageInfo,
): Triple<Int, Int?, Int?> {
    val currentPage = resp.page ?: pi.page ?: fallbackPage
    val totalPages = resp.totalPages ?: pi.totalPages
    val nextPage = resp.nextPage ?: pi.nextPage
    return Triple(currentPage, totalPages, nextPage)
}

private fun computeNextPage(
    currentPage: Int,
    totalPages: Int?,
    hintedNextPage: Int?,
): Int? =
    hintedNextPage
        ?: if (totalPages != null && currentPage < totalPages) currentPage + 1 else null

private fun logHopResponse(
    hop: Int,
    count: Int,
    page: Int?,
    totalPages: Int?,
    nextPage: Int?,
) {
    Log.d(
        ORDERS_TAG,
        "← hop#$hop response: items=$count, page=$page, totalPages=$totalPages, nextPage=$nextPage",
    )
}

private fun extractOrdersAndPageInfo(data: JsonElement?): Pair<List<Order>, PageInfo> {
    if (data == null || data.isJsonNull) return emptyList<Order>() to PageInfo()

    val gson = Gson()
    val listType = object : TypeToken<List<Order>>() {}.type

    return when {
        data.isJsonArray -> {
            val items: List<Order> = gson.fromJson(data, listType) ?: emptyList()
            items to PageInfo()
        }

        data.isJsonObject -> {
            val obj: JsonObject = data.asJsonObject

            val (arr, usedKey) = obj.findFirstItemsArray()
            val items: List<Order> =
                if (arr != null) gson.fromJson(arr, listType) ?: emptyList() else emptyList()

            val pageInfo = obj.extractPageInfo()

            Log.d(
                ORDERS_TAG,
                "data.shape=object, arrayKey=$usedKey, items=${items.size}",
            )
            items to pageInfo
        }

        else -> emptyList<Order>() to PageInfo()
    }
}

private fun JsonObject.findFirstItemsArray(): Pair<JsonArray?, String?> {
    var foundArray: JsonArray? = null
    var usedKey: String? = null

    // 1) Preferred keys
    for (k in PREFERRED_ARRAY_KEYS) {
        if (has(k) && get(k).isJsonArray) {
            foundArray = getAsJsonArray(k)
            usedKey = k
            break
        }
    }

    // 2) Fallback: first array field
    if (foundArray == null) {
        for ((k, v) in entrySet()) {
            if (v.isJsonArray) {
                foundArray = v.asJsonArray
                usedKey = k
                break
            }
        }
    }

    return foundArray to usedKey
}

private fun JsonObject.extractPageInfo(): PageInfo {
    val pageInfoObj =
        if (has("pageInfo") && get("pageInfo").isJsonObject) getAsJsonObject("pageInfo") else null

    return PageInfo(
        page = this.optInt("page") ?: pageInfoObj?.optInt("page"),
        totalPages = this.optInt("totalPages") ?: pageInfoObj?.optInt("totalPages"),
        nextPage = this.optInt("nextPage") ?: pageInfoObj?.optInt("nextPage"),
        hasMore = this.optBool("hasMore") ?: pageInfoObj?.optBool("hasNextPage"),
        cursor =
            this.optString("cursor")
                ?: pageInfoObj?.optString("cursor")
                ?: pageInfoObj?.optString("endCursor"),
        nextCursor = this.optString("nextCursor") ?: pageInfoObj?.optString("endCursor"),
    )
}

private fun JsonObject.optString(key: String): String? =
    if (has(key) && get(key).isJsonPrimitive && get(key).asJsonPrimitive.isString) get(key).asString else null

private fun JsonObject.optInt(key: String): Int? =
    if (has(key) && get(key).isJsonPrimitive && get(key).asJsonPrimitive.isNumber) get(key).asInt else null

private fun JsonObject.optBool(key: String): Boolean? =
    if (has(key) && get(key).isJsonPrimitive && get(key).asJsonPrimitive.isBoolean) get(key).asBoolean else null

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
import com.ntg.lmd.mainscreen.data.model.PageInfo
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
    // fetch all live orders with pagination support
    override suspend fun getAllLiveOrders(pageSize: Int): Result<List<Order>> =
        try {
            val all = mutableListOf<Order>()
            var page = 1

            // loop until API says no more pages
            while (true) {
                val resp = fetchPage(liveOrdersApi, page, pageSize)

                // extract orders and pagination info from the API response
                val (items, pi) = extractOrdersAndPageInfo(resp.data)
                all += items
                val (currentPage, totalPages, hintedNext) = resolvePageMeta(resp, page, pi)

                // decide what the next page should be
                val next = computeNextPage(currentPage, totalPages, hintedNext)

                // stop if no items or no next page
                if (items.isEmpty() || next == null) {
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

    // Connect to socket channel and start listening for live order updates
    override fun connectToOrders(channelName: String) {
        socket.connect(channelName)
        socket.startChannelListener()
    }

    // disconnect socket connection
    override fun disconnectFromOrders() {
        socket.disconnect()
    }

    // retry socket connection after failure
    override fun retryConnection() {
        socket.retryConnection()
    }

    // send request to update order status through socket
    override fun updateOrderStatus(
        orderId: String,
        status: String,
    ) {
        socket.updateOrderStatus(orderId, status)
    }

    // expose live orders from socket as a flow
    fun orders() = socket.orders
}

private val PREFERRED_ARRAY_KEYS =
    listOf(
        "items",
        "orders",
        "initial_orders",
        "results",
        "rows",
        "list",
        "liveOrders",
        "data",
    )

// call API endpoint to fetch a single page of orders
private suspend fun fetchPage(
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

// resolve current page, total pages, and hinted text page
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

// decide the next page number to fetch
private fun computeNextPage(
    currentPage: Int,
    totalPages: Int?,
    hintedNextPage: Int?,
): Int? =
    hintedNextPage
        ?: if (totalPages != null && currentPage < totalPages) currentPage + 1 else null

// extract orders and page info from JSON
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

// search JSON object for an array of orders
private fun JsonObject.findFirstItemsArray(): Pair<JsonArray?, String?> {
    var foundArray: JsonArray? = null
    var usedKey: String? = null

    for (k in PREFERRED_ARRAY_KEYS) {
        if (has(k) && get(k).isJsonArray) {
            foundArray = getAsJsonArray(k)
            usedKey = k
            break
        }
    }

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

// extract pagination info
private fun JsonObject.extractPageInfo(): PageInfo {
    val pageInfoObj =
        when {
            has("pageInfo") && get("pageInfo").isJsonObject -> getAsJsonObject("pageInfo")
            has("pagination") && get("pagination").isJsonObject -> getAsJsonObject("pagination")
            else -> null
        }

    return PageInfo(
        page = this.optInt("page")
            ?: pageInfoObj?.optInt("page")
            ?: pageInfoObj?.optInt("current_page"),
        totalPages = this.optInt("totalPages")
            ?: pageInfoObj?.optInt("totalPages")
            ?: pageInfoObj?.optInt("total_pages"),
        nextPage = this.optInt("nextPage")
            ?: pageInfoObj?.optInt("nextPage"),
        hasMore = this.optBool("hasMore")
            ?: pageInfoObj?.optBool("hasNextPage")
            ?: pageInfoObj?.optBool("has_next_page"),
        cursor = this.optString("cursor")
            ?: pageInfoObj?.optString("cursor")
            ?: pageInfoObj?.optString("endCursor"),
        nextCursor = this.optString("nextCursor")
            ?: pageInfoObj?.optString("endCursor"),
    )
}

// access extensions
private fun JsonObject.optString(key: String): String? =
    if (has(key) && get(key).isJsonPrimitive && get(key).asJsonPrimitive.isString) get(key).asString else null

private fun JsonObject.optInt(key: String): Int? =
    if (has(key) && get(key).isJsonPrimitive && get(key).asJsonPrimitive.isNumber) get(key).asInt else null

private fun JsonObject.optBool(key: String): Boolean? =
    if (has(key) && get(key).isJsonPrimitive && get(key).asJsonPrimitive.isBoolean) get(key).asBoolean else null

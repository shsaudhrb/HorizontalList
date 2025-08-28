package com.ntg.lmd.order.data.remote.repository

import android.util.Log
import com.ntg.lmd.order.data.remote.OrdersHistoryApi
import com.ntg.lmd.order.data.remote.dto.toUi
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.order.domain.model.repository.OrdersRepository
import retrofit2.HttpException
import java.io.IOException

class OrdersRepositoryImpl(
    private val api: OrdersHistoryApi
) : OrdersRepository {

    override suspend fun getOrders(token: String, page: Int, limit: Int): List<OrderHistoryUi> {
        return try {
            val response = api.getOrders("Bearer $token", page = page, limit = limit)

            if (response.success) {
                response.data.orders.map { it.toUi() }
            } else {
                Log.e("OrdersRepository", "API responded with success=false, error=${response.error}")
                emptyList()
            }

        } catch (e: HttpException) {
            Log.e("OrdersRepository", "HTTP error: ${e.code()} ${e.message()}")
            emptyList()
        } catch (e: IOException) {
            Log.e("OrdersRepository", "Network error: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e("OrdersRepository", "Unexpected error: ${e.message}", e)
            emptyList()
        }
    }
}

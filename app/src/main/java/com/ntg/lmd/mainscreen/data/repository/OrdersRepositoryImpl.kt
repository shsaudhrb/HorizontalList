package com.ntg.lmd.mainscreen.data.repository

import android.util.Log
import com.ntg.lmd.mainscreen.data.datasource.remote.LiveOrdersApiService
import com.ntg.lmd.mainscreen.data.model.LiveOrdersResponse
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import com.ntg.lmd.network.sockets.SocketIntegration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class OrdersRepositoryImpl(
    private val liveOrdersApi: LiveOrdersApiService,
    private val socket: SocketIntegration,
) : OrdersRepository {

    override suspend fun getLiveOrders(): Flow<Result<LiveOrdersResponse>> = flow {
        try {
            val response = liveOrdersApi.getLiveOrders()
            if (response.success && response.data != null) {
                emit(Result.success(response))
            } else {
                emit(Result.failure(Exception("Failed to get live orders")))
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                emit(Result.failure(Exception("Authentication failed after token refresh attempt")))
            } else {
                emit(Result.failure(Exception("Server error: ${e.code()}")))
            }
        } catch (e: IOException) {
            emit(Result.failure(Exception("Network error: ${e.message}")))
        } catch (e: Exception) {
            emit(Result.failure(Exception("Unexpected error: ${e.message}")))
        }
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

    override fun updateOrderStatus(orderId: String, status: String) {
        socket.updateOrderStatus(orderId, status)
    }

    fun connectionState() = socket.connectionState
    fun orders() = socket.orders
    fun orderChannel() = socket.getOrderChannel()

    fun destroy() {
        try {
            socket.destroy()
        } catch (e: Exception) {
            Log.e("OrdersRepository", "Error destroying WebSocketManager", e)
        }
    }
}

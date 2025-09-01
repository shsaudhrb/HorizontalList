package com.ntg.lmd.mainscreen.data.datasource.remote

import com.ntg.lmd.mainscreen.data.model.UpdateOrderStatusEnvelope
import com.ntg.lmd.mainscreen.data.model.UpdateOrderStatusRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface UpdatetOrdersStatusApi {
    @POST("update-order-status")
    suspend fun updateOrderStatus(
        @Body body: UpdateOrderStatusRequest,
    ): UpdateOrderStatusEnvelope
}

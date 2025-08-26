package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.domain.model.DeliveryLogDomain

interface OrderLogRepository {
    suspend fun loadLogs(): List<DeliveryLogDomain>
}

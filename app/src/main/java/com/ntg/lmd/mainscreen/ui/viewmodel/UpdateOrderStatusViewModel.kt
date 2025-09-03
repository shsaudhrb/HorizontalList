package com.ntg.lmd.mainscreen.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.model.toApiId
import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
import com.ntg.lmd.utils.SecureUserStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class UpdateOrderStatusViewModel(
    private val updateStatus: UpdateOrderStatusUseCase,
    private val userStore: SecureUserStore,
) : ViewModel() {
    private val _updatingIds = MutableStateFlow<Set<String>>(emptySet())
    val updatingIds: StateFlow<Set<String>> = _updatingIds

    private val _success = MutableSharedFlow<OrderInfo>()
    val success: SharedFlow<OrderInfo> = _success
    private val _currentUserId = MutableStateFlow<String?>(userStore.getUserId())
    val currentUserId: StateFlow<String?> = _currentUserId
    private val _error = MutableSharedFlow<Pair<String, () -> Unit>>()
    val error: SharedFlow<Pair<String, () -> Unit>> = _error

    init {
        userStore.onUserChanged = { id -> _currentUserId.value = id }
    }

    fun update(
        orderId: String,
        targetStatus: OrderStatus,
        assignedAgentId: String? = null,
    ) {
        _updatingIds.update { it + orderId }
        viewModelScope.launch {
            try {
                OrderLogger.postStart(orderId, targetStatus)
                runCatching { updateStatus(orderId, targetStatus.toApiId(), assignedAgentId) }
                    .onSuccess { serverOrder ->
                        OrderLogger.postSuccess(orderId, serverOrder.status)
                        _success.emit(serverOrder)
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        OrderLogger.postError(orderId, targetStatus, e)
                        _error.emit(e.toUserMessage() to {
                            update(
                                orderId,
                                targetStatus,
                                assignedAgentId
                            )
                        })
                    }
            } finally {
                _updatingIds.update { it - orderId }
            }
        }
    }

    object OrderLogger {
        private const val TAG = "OrderAction"

        fun uiTap(
            orderId: String,
            orderNumber: String?,
            action: String,
        ) {
            Log.d(TAG, "UI → tap action=$action | orderId=$orderId orderNo=${orderNumber ?: "-"}")
        }

        fun postStart(
            orderId: String,
            target: OrderStatus,
        ) {
            Log.i(TAG, "API → POST /update-order-status start | orderId=$orderId target=$target")
        }

        fun postSuccess(
            orderId: String,
            new: OrderStatus?,
        ) {
            Log.i(TAG, "API ← success | orderId=$orderId newStatus=$new")
        }

        fun postError(
            orderId: String,
            target: OrderStatus,
            t: Throwable,
        ) {
            Log.e(TAG, "API ← error | orderId=$orderId target=$target msg=${t.message}", t)
        }
    }
}

package com.ntg.lmd.order.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.order.domain.model.usecase.GetOrdersUseCase

class OrderHistoryViewModelFactory(
    private val getOrdersUseCase: GetOrdersUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderHistoryViewModel(getOrdersUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

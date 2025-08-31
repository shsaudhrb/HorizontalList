// MyOrdersViewModelFactory.kt
package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase

class MyOrdersViewModelFactory(
    private val getMyOrders: GetMyOrdersUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MyOrdersViewModel::class.java))
        return MyOrdersViewModel(getMyOrders) as T
    }
}

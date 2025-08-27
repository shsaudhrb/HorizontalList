package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.data.repository.OrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.network.core.RetrofitProvider

class MyPoolVMFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = OrdersRepositoryImpl(RetrofitProvider.ordersApi)
        val usecase = GetMyOrdersUseCase(repo)
        return MyPoolViewModel(usecase) as T
    }
}

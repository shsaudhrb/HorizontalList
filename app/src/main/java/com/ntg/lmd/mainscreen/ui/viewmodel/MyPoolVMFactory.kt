package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.data.repository.MyOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.network.core.RetrofitProvider

class MyPoolVMFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = MyOrdersRepositoryImpl(RetrofitProvider.ordersApi)
        val getMyOrdersUseCase = GetMyOrdersUseCase(repo)
        val computeDistancesUseCase = ComputeDistancesUseCase()
        return MyPoolViewModel(getMyOrdersUseCase, computeDistancesUseCase) as T
    }
}

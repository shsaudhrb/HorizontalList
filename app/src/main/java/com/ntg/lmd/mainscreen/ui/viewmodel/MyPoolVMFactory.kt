package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.data.repository.OrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase

class MyPoolVMFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = OrdersRepositoryImpl()
        val usecase = GetMyOrdersUseCase(repo)
        return MyPoolViewModel(usecase) as T
    }
}

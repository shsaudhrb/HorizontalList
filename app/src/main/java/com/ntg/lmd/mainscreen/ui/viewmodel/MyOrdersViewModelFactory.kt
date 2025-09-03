package com.ntg.lmd.mainscreen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.data.repository.MyOrdersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.usecase.ComputeDistancesUseCase
import com.ntg.lmd.mainscreen.domain.usecase.GetMyOrdersUseCase
import com.ntg.lmd.network.core.RetrofitProvider

@Suppress("UnusedPrivateProperty")
class MyOrdersViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val api = RetrofitProvider.ordersApi
        val repo = MyOrdersRepositoryImpl(api)
        val getMyOrders = GetMyOrdersUseCase(repo)
        val computeDistancesUseCase = ComputeDistancesUseCase()
        val uid = RetrofitProvider.userStore.getUserId()
        return MyOrdersViewModel(getMyOrders, computeDistancesUseCase, uid) as T
    }
}

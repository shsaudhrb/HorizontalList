package com.ntg.lmd.mainscreen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.data.repository.UpdateOrdersStatusRepositoryImpl
import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
import com.ntg.lmd.network.core.RetrofitProvider
import com.ntg.lmd.utils.SecureUserStore

class UpdateOrderStatusViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userStore = SecureUserStore(application.applicationContext)
        val api = RetrofitProvider.updateStatusApi
        val repo = UpdateOrdersStatusRepositoryImpl(api)
        val usecase = UpdateOrderStatusUseCase(repo)
        return UpdateOrderStatusViewModel(usecase, userStore) as T
    }
}

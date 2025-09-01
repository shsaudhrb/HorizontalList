package com.ntg.lmd.mainscreen.ui.viewmodel

import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase
import com.ntg.lmd.utils.SecureUserStore

class UpdateOrderStatusViewModelFactory(
    private val usecase: UpdateOrderStatusUseCase,
    private val userStore: SecureUserStore,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = UpdateOrderStatusViewModel(usecase, userStore) as T
}

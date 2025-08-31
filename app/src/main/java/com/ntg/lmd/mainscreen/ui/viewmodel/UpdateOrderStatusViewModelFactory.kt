package com.ntg.lmd.mainscreen.ui.viewmodel

import com.ntg.lmd.mainscreen.domain.usecase.UpdateOrderStatusUseCase

class UpdateOrderStatusViewModelFactory(
    private val usecase: UpdateOrderStatusUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return UpdateOrderStatusViewModel(usecase) as T
    }
}

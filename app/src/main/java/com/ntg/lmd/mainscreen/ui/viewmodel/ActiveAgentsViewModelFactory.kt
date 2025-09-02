package com.ntg.lmd.mainscreen.ui.viewmodel

import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase

class ActiveAgentsViewModelFactory(
    private val usecase: GetActiveUsersUseCase,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ActiveAgentsViewModel(usecase) as T
}

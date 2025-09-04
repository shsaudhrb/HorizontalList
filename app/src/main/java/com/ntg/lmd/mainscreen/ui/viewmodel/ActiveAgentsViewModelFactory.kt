package com.ntg.lmd.mainscreen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.mainscreen.data.repository.UsersRepositoryImpl
import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase
import com.ntg.lmd.network.core.RetrofitProvider

@Suppress("detekt.UnusedPrivateMember")
class ActiveAgentsViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val api = RetrofitProvider.usersApi
        val repo = UsersRepositoryImpl(api)
        val usecase = GetActiveUsersUseCase(repo)
        return ActiveAgentsViewModel(usecase) as T
    }
}

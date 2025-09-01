package com.ntg.lmd.authentication.ui.viewmodel.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.MyApp

class LoginViewModelFactory(
    private val app: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val myApp = app as MyApp
        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(myApp.authRepo) as T
    }
}

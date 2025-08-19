package com.example.lmd.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lmd.authentication.ui.screens.login.loginScreen
import com.example.lmd.authentication.ui.screens.register.registerScreen
import com.example.lmd.authentication.ui.screens.splash.splashScreen

@Composable
fun appNavGraph(
    navController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {
            splashScreen(
                navController = navController
            )
        }

        composable(Screen.Login.route) {
            loginScreen(
                navController = navController
            )
        }

        composable(Screen.Register.route) {
            registerScreen(
                navController = navController
            )
        }

    }

}
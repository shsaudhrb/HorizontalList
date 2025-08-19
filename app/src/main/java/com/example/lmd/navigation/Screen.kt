package com.example.lmd.navigation

sealed class Screen(
    val route: String,
) {
    object Splash : Screen("splash_screen")

    object Register : Screen("register_screen")

    object Login : Screen("login_screen")
}
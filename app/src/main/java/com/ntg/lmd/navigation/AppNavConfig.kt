package com.ntg.lmd.navigation

import androidx.navigation.NavHostController

// ---------- Minimal app-level nav config ----------
data class AppNavConfig(
    val navController: NavHostController,
    val currentRoute: String?,
)
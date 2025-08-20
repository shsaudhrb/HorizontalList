package com.ntg.lmd.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ntg.lmd.authentication.ui.screens.login.loginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen
import com.ntg.lmd.mainscreen.ui.screens.deliveriesLogScreen
import com.ntg.lmd.mainscreen.ui.screens.generalPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.myOrdersScreen
import com.ntg.lmd.mainscreen.ui.screens.myPoolScreen
import com.ntg.lmd.mainscreen.ui.screens.ordersHistoryScreen
import com.ntg.lmd.notification.ui.screens.notificationScreen
import com.ntg.lmd.settings.ui.screens.settingsOptions

@Composable
fun appNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {
        // ---------- Splash & Auth ----------
        composable(Screen.Splash.route) {
            splashScreen(
                navController = navController,
            )
        }

        composable(Screen.Login.route) {
            loginScreen()
        }

        composable(Screen.Register.route) {
            registerScreen(
                navController = navController,
            )
        }

        // ---------- Main Screen ----------

        composable(Screen.DeliveriesLog.route) {
            deliveriesLogScreen()
        }

        composable(Screen.GeneralPool.route) {
            generalPoolScreen(
                navController = navController,
            )
        }

        composable(Screen.MyOrders.route) {
            myOrdersScreen()
        }

        composable(Screen.MyPool.route) {
            myPoolScreen()
        }

        composable(Screen.OrdersHistory.route) {
            ordersHistoryScreen()
        }

        // ---------- Notification & Settings ----------

        composable(Screen.Notifications.route) {
            notificationScreen()
        }

        composable(Screen.Settings.route) {
            settingsOptions()
        }
    }
}

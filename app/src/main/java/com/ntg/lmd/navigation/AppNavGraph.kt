package com.ntg.lmd.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ntg.lmd.authentication.ui.screens.login.loginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen
import com.ntg.lmd.authentication.ui.screens.reset.enterEmailResetPasswordScreen
import com.ntg.lmd.authentication.ui.screens.reset.newPasswordScreen
import com.ntg.lmd.authentication.ui.screens.reset.verificationCodeScreen
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
            loginScreen(
                navController = navController,
            )
        }

        composable(Screen.Register.route) {
            registerScreen(
                navController = navController,
            )
        }

        // ---------- Reset Password ----------

        composable(Screen.EnterEmailResetPassword.route) {
            enterEmailResetPasswordScreen(
                navController = navController,
            )
        }

        composable(Screen.VerificationCode.route) {
            verificationCodeScreen(
                navController = navController,
            )
        }

        composable(Screen.NewPassword.route) {
            newPasswordScreen(
                navController = navController,
            )
        }

        // ---------- Main Screen ----------

        composable(Screen.DeliveriesLog.route) {
            deliveriesLogScreen(
                navController = navController,
            )
        }

        composable(Screen.GeneralPool.route) {
            generalPoolScreen(
                navController = navController,
            )
        }

        composable(Screen.MyOrders.route) {
            myOrdersScreen(
                navController = navController,
            )
        }

        composable(Screen.MyPool.route) {
            myPoolScreen(
                navController = navController,
            )
        }

        composable(Screen.OrdersHistory.route) {
            ordersHistoryScreen(
                navController = navController,
            )
        }

        // ---------- Notification & Settings ----------

        composable(Screen.Notifications.route) {
            notificationScreen(
                navController = navController,
            )
        }

        composable(Screen.Settings.route) {
            settingsOptions(
                navController = navController,
            )
        }
    }
}

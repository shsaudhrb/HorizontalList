package com.ntg.lmd.navigation

sealed class Screen(
    val route: String,
) {
    // ---------- Splash & Auth ----------

    object Splash : Screen("splash_screen")

    object Register : Screen("register_screen")

    object Login : Screen("login_screen")

    // ---------- Reset Password ----------

    object EnterEmailResetPassword : Screen("enter_email_reset_password_screen")

    object VerificationCode : Screen("verification_code_screen")

    object NewPassword : Screen("new_password_screen")

    // ---------- Main Screen ----------

    object DeliveriesLog : Screen("deliveries_log_screen")

    object GeneralPool : Screen("general_pool_screen")

    object MyOrders : Screen("my_orders_screen")

    object MyPool : Screen("my_pool_screen")

    object OrdersHistory : Screen("orders_history_screen")

    // ---------- Notification & Settings ----------

    object Notifications : Screen("notifications_screen")

    object Settings : Screen("settings_screen")
}

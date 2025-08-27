package com.ntg.lmd.navigation

sealed class Screen(
    val route: String,
) {
    // ---------- Splash & Auth ----------

    object Splash : Screen("splash_screen")

    object Register : Screen("register_screen")

    object Login : Screen("login_screen")

    // ---------- Main Screen ----------

    object Drawer : Screen("root_drawer")

    object DeliveriesLog : Screen("deliveries_log_screen")

    object GeneralPool : Screen("general_pool_screen")

    object MyOrders : Screen("my_orders_screen")

    object MyPool : Screen("my_pool_screen")

    object OrdersHistory : Screen("orders_history_screen")

    data object Chat : Screen("chat_screen")

    data object Logout : Screen("logout_screen")

    // ---------- Notification & Settings ----------

    object Notifications : Screen("notifications_screen")

    object Settings : Screen("settings_screen")

    // ---------- Details ----------
    object OrderDetails : Screen("order_details/{orderId}") {
        fun route(orderId: Long) = "order_details/$orderId"
    }
}

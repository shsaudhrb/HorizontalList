package com.ntg.lmd.navigation.component

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.ntg.lmd.R
import com.ntg.lmd.navigation.Screen

data class DrawerItem(
    @StringRes val labelRes: Int,
    val route: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val badgeCount: Int? = null
)

val drawerItems = listOf(
    DrawerItem(R.string.menu_general_pool,    Screen.GeneralPool.route,   Icons.Filled.AllInbox),
    DrawerItem(R.string.menu_my_orders,       Screen.MyOrders.route,      Icons.Filled.ShoppingCart, badgeCount = 6),
    DrawerItem(R.string.menu_order_history,   Screen.OrdersHistory.route, Icons.Filled.History),
    DrawerItem(R.string.menu_notifications,   Screen.Notifications.route, Icons.Filled.Notifications),
    DrawerItem(R.string.menu_deliveries_log,  Screen.DeliveriesLog.route, Icons.Filled.LocalShipping),
    DrawerItem(R.string.menu_settings,        Screen.Settings.route,      Icons.Filled.Settings),
    DrawerItem(R.string.menu_chat,            Screen.Chat.route,          Icons.Filled.Chat),
    DrawerItem(R.string.menu_logout,          Screen.Logout.route,        Icons.Filled.PowerSettingsNew),
)

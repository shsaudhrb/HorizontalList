package com.ntg.lmd

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.authentication.ui.components.NotificationPermissionState
import com.ntg.lmd.navigation.appNavGraph
import com.ntg.lmd.notification.data.dataSource.remote.FcmNotificationHelper
import com.ntg.lmd.notification.ui.viewmodel.DeepLinkViewModel
import com.ntg.lmd.notification.ui.viewmodel.NotificationsVMFactory
import com.ntg.lmd.notification.ui.viewmodel.NotificationsViewModel
import com.ntg.lmd.ui.theme.lmdTheme
import com.ntg.lmd.utils.LocaleHelper.applyLanguage

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val sp = newBase.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val langCode = sp.getString("lang", "en") ?: "en"
        val localized = applyLanguage(newBase, langCode, recreateActivity = false)
        super.attachBaseContext(localized)
    }

    private lateinit var navController: NavHostController
    private val deepLinkVM by viewModels<DeepLinkViewModel>()
    private val notificationsVM by viewModels<NotificationsViewModel> {
        NotificationsVMFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // capture the initial intent BEFORE composing
        deepLinkVM.setFromIntent(intent)

        enableEdgeToEdge()
        setContent {
            lmdTheme {
                navController = rememberNavController()
                // pass the VM down
                appNavGraph(
                    rootNavController = navController,
                    deeplinkVM = deepLinkVM,
                    notificationsVM = notificationsVM,
                )
                // keep this for normal deep link handling too
                LaunchedEffect(Unit) { navController.handleDeepLink(intent) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // update VM and let NavController handle it
        deepLinkVM.setFromIntent(intent)
        if (::navController.isInitialized) {
            navController.handleDeepLink(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val canPost = FcmNotificationHelper.canPostNotifications(this)
            if (!canPost) {
                val deniedPermanently =
                    !shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                if (deniedPermanently) {
                    notificationsVM.updatePermissionState(NotificationPermissionState.DENIED_PERMANENTLY)
                } else {
                    notificationsVM.updatePermissionState(NotificationPermissionState.DENIED)
                }
            } else {
                notificationsVM.updatePermissionState(NotificationPermissionState.GRANTED)
            }
        } else {
            notificationsVM.updatePermissionState(NotificationPermissionState.GRANTED)
        }
    }
}

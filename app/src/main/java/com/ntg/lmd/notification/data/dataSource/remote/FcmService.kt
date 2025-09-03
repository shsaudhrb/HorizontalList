package com.ntg.lmd.notification.data.dataSource.remote

import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ntg.lmd.R
import com.ntg.lmd.network.core.RetrofitProvider.userStore
import com.ntg.lmd.notification.data.model.FCMServiceLocator
import com.ntg.lmd.notification.domain.model.AgentNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.ntg.lmd.notification.data.model.NotificationPayload as Payload

class FcmService : FirebaseMessagingService() {
    private fun isLoggedIn(): Boolean = userStore.getUserId()?.isNotBlank() == true

    companion object {
        private const val TAG = "FcmService"
    }

    override fun onNewToken(token: String) {
        val autoInit = FirebaseMessaging.getInstance().isAutoInitEnabled
        val loggedIn = isLoggedIn()
        Log.d(TAG, "onNewToken called; loggedIn=$loggedIn autoInit=$autoInit")
        if (!loggedIn || !autoInit) {
            Log.w(TAG, "IGNORED new token (loggedIn=$loggedIn autoInit=$autoInit)")
            return
        }
        Log.i(TAG, "Accepted FCM token: $token")
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        Log.d(TAG, "FCM message received: ${msg.messageId}")
        Log.d(TAG, "Message data: ${msg.data}")
        Log.d(TAG, "Message notification: ${msg.notification}")

        val loggedIn = isLoggedIn()
        val autoInit = FirebaseMessaging.getInstance().isAutoInitEnabled
        Log.d(TAG, "onMessageReceived id=${msg.messageId} loggedIn=$loggedIn autoInit=$autoInit")
        if (!loggedIn) {
            Log.w(TAG, "IGNORED push (logged out)")
            return
        }

        val payload = parsePayload(msg) ?: run {
            Log.w(TAG, "Ignored push: invalid/empty payload")
            return
        }
        Log.d(TAG, "Parsed payload: title=${payload.title}, body=${payload.body}, type=${payload.type}")

        persistAgentNotification(payload.type, payload.body)
        showNotificationIfAllowed(payload)
    }

    private fun showNotificationIfAllowed(payload: Payload) {
        CoroutineScope(Dispatchers.IO).launch {
            if (FcmNotificationHelper.canPostNotifications(this@FcmService)) {
                try {
                    Log.d(TAG, "SHOWING local notification")
                    val notif = FcmNotificationHelper.buildNotification(
                        context = this@FcmService,
                        title = payload.title,
                        body = payload.body,
                        deepLink = payload.deepLink,
                    )
                    NotificationManagerCompat.from(this@FcmService).notify(Random.nextInt(), notif)
                    Log.d(TAG, "Local notification shown successfully")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Failed to show local notification due to security exception", e)
                }
            } else {
                Log.w(TAG, "Cannot post notifications - permission denied or disabled")
            }
        }
    }

    private fun parsePayload(msg: RemoteMessage): Payload? {
        val notif = msg.notification
        val d = msg.data
        val title = d["title"] ?: notif?.title ?: getString(R.string.notification_title)
        val body = d["message"] ?: d["body"] ?: notif?.body ?: return null
        val deepLink = d["deeplink"] ?: FcmNotificationHelper.DEFAULT_DEEPLINK
        val type =
            when (d["type"]?.lowercase()) {
                "order", "order_status" -> AgentNotification.Type.ORDER_STATUS
                "wallet", "payment" -> AgentNotification.Type.WALLET
                else -> AgentNotification.Type.OTHER
            }
        return Payload(title = title, body = body, deepLink = deepLink, type = type)
    }

    private fun persistAgentNotification(
        type: AgentNotification.Type,
        body: String,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            FCMServiceLocator.saveIncomingNotificationUseCase(
                AgentNotification(
                    id = 0L,
                    message = body,
                    type = type,
                    timestampMs = System.currentTimeMillis(),
                ),
            )
        }
    }
}

package com.ntg.lmd.notification.data.dataSource.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ntg.lmd.MainActivity
import com.ntg.lmd.R
import com.ntg.lmd.notification.data.model.FCMServiceLocator
import com.ntg.lmd.notification.domain.model.AgentNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.ntg.lmd.notification.data.model.NotificationPayload as Payload

class FcmService : FirebaseMessagingService() {
    companion object {
        private const val CHANNEL_ID = "agent_updates"
        private const val CHANNEL_NAME = "Agent Updates"
        private const val CHANNEL_DESC = "Notifications about agent updates and orders"
        private const val TAG = "FcmService"

        private const val REQUEST_CODE_CONTENT = 2001
        private const val DEFAULT_DEEPLINK = "myapp://notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        Log.i(TAG, "New FCM token generated: $token")
        
    }

 
     // manually get and log the current FCM token
   
    fun getCurrentToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "Current FCM Token: $token")
                    Log.i(TAG, "Successfully retrieved current FCM token")
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                }
            }
    }



    override fun onMessageReceived(msg: RemoteMessage) {
        Log.d(TAG, "FCM message received: ${msg.messageId}")
        Log.d(TAG, "Message data: ${msg.data}")
        Log.d(TAG, "Message notification: ${msg.notification}")
        
        val notif = msg.notification
        val data = msg.data
        if (notif == null && data.isNullOrEmpty()) {
            Log.w(TAG, "Message received but no notification or data found")
            return
        }

        val payload = parsePayload(notif, data) ?: return
        Log.d(TAG, "Parsed payload: title=${payload.title}, body=${payload.body}, type=${payload.type}")

        // Save to repository
        persistAgentNotification(payload.type, payload.body)

        // Show local notification
        CoroutineScope(Dispatchers.IO).launch {
            if (canPostNotifications()) {
                try {
                    showLocalNotification(
                        title = payload.title,
                        body = payload.body,
                        deepLink = payload.deepLink,
                    )
                    Log.d(TAG, "Local notification shown successfully")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Failed to show local notification due to security exception", e)
                }
            } else {
                Log.w(TAG, "Cannot post notifications - permission denied or disabled")
            }
        }
    }

    private fun parsePayload(
        notif: RemoteMessage.Notification?,
        data: Map<String, String>?,
    ): Payload? {
        val d = data ?: emptyMap()

        val title = d["title"] ?: notif?.title ?: getString(R.string.notification_title)
        val body = d["message"] ?: d["body"] ?: notif?.body ?: return null
        val deepLink = d["deeplink"] ?: DEFAULT_DEEPLINK
        val type = parseType(d["type"])

        return Payload(title = title, body = body, deepLink = deepLink, type = type)
    }

    private fun parseType(raw: String?): AgentNotification.Type =
        when (raw?.lowercase()) {
            "order", "order_status" -> AgentNotification.Type.ORDER_STATUS
            "wallet", "payment" -> AgentNotification.Type.WALLET
            else -> AgentNotification.Type.OTHER
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showLocalNotification(
        title: String,
        body: String,
        deepLink: String,
    ) {
        ensureChannel()
        val contentIntent = buildContentIntent(deepLink)
        val notification = buildNotification(title, body, contentIntent)
        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }

    private fun buildContentIntent(deepLink: String): PendingIntent {
        val clickIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(deepLink),
                this,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_CONTENT,
            clickIntent,
            pendingIntentFlags(),
        )
    }

    private fun buildNotification(
        title: String,
        body: String,
        contentIntent: PendingIntent,
    ): android.app.Notification {
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_notification)
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun pendingIntentFlags(): Int {
        val base = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            base or PendingIntent.FLAG_IMMUTABLE
        } else {
            base
        }
    }

    private fun canPostNotifications(): Boolean {
        val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (!enabled) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
            nm.createNotificationChannel(channel)
        }
    }
}

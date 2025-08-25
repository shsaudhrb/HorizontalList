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
import com.ntg.lmd.notification.data.repository.NotificationRepositoryImpl
import com.ntg.lmd.notification.domain.model.AgentNotification
import com.ntg.lmd.notification.domain.repository.NotificationRepository
import com.ntg.lmd.notification.domain.usecase.ObserveNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.RefreshNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.SaveIncomingNotificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class FcmService : FirebaseMessagingService() {
    companion object {
        private const val CHANNEL_ID = "agent_updates"
        private const val CHANNEL_NAME = "Agent Updates"
        private const val CHANNEL_DESC = "Notifications about agent updates and orders"
        private const val TAG = "FCM"

        // ✅ No magic numbers
        private const val REQUEST_CODE_CONTENT = 2001
        private const val DEFAULT_DEEPLINK = "myapp://notifications"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken = $token")
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val notif = msg.notification
        val data = msg.data
        if (notif == null && data.isNullOrEmpty()) return

        val payload = parsePayload(notif, data) ?: return

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
                } catch (_: SecurityException) {
                    // ignored
                }
            }
        }
    }

    // ------- Parsing / persistence helpers (cuts down cyclomatic complexity) -------

    private data class Payload(
        val title: String,
        val body: String,
        val deepLink: String,
        val type: AgentNotification.Type,
    )

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
            ServiceLocator.saveIncomingNotificationUseCase(
                AgentNotification(
                    id = 0L,
                    message = body,
                    type = type,
                    timestampMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    // ---------------- Local notification ----------------

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showLocalNotification(
        title: String,
        body: String,
        deepLink: String,
    ) {
        ensureChannel()

        val clickIntent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(deepLink),
                this,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val contentIntent =
            PendingIntent.getActivity(
                this,
                REQUEST_CODE_CONTENT, // ✅ no magic number
                clickIntent,
                flags,
            )

        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Optional large icon
        val large = BitmapFactory.decodeResource(resources, R.drawable.ic_notification)
        builder.setLargeIcon(large)

        NotificationManagerCompat.from(this).notify(Random.nextInt(), builder.build())
    }

    /** فحص إذن الإشعارات + تفعيلها للتطبيق */
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

    /** إنشاء القناة (Android O+) */
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

object ServiceLocator {
    private val repo: NotificationRepository by lazy { NotificationRepositoryImpl() }

    val observeNotificationsUseCase by lazy { ObserveNotificationsUseCase(repo) }
    val refreshNotificationsUseCase by lazy { RefreshNotificationsUseCase(repo) }
    val saveIncomingNotificationUseCase by lazy { SaveIncomingNotificationUseCase(repo) }

    fun notificationsRepo(): NotificationRepository = repo
}

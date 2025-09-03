package com.ntg.lmd.notification.data.dataSource.remote
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ntg.lmd.MainActivity
import com.ntg.lmd.R

object FcmNotificationHelper {
    private const val TAG = "FcmNotifHelper"

    const val CHANNEL_ID = "agent_updates"
    private const val CHANNEL_NAME = "Agent Updates"
    private const val CHANNEL_DESC = "Notifications about agent updates and orders"

    const val DEFAULT_DEEPLINK = "myapp://notifications"
    private const val REQUEST_CODE_CONTENT = 2001

    fun canPostNotifications(context: Context): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) {
            Log.w(TAG, "Notifications disabled by user/app settings")
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!granted) Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            granted
        } else {
            true
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun buildNotification(
        context: Context,
        title: String,
        body: String,
        deepLink: String,
    ): android.app.Notification {
        ensureChannel(context)
        val contentIntent = buildContentIntent(context, deepLink)
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification)
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
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

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            Log.d(TAG, "Creating notification channel: $CHANNEL_ID")
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

    private fun buildContentIntent(
        context: Context,
        deepLink: String,
    ): PendingIntent {
        val click =
            android.content
                .Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse(deepLink),
                    context,
                    MainActivity::class.java,
                ).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
        val base = PendingIntent.FLAG_UPDATE_CURRENT
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                base or PendingIntent.FLAG_IMMUTABLE
            } else {
                base
            }

        return PendingIntent.getActivity(context, REQUEST_CODE_CONTENT, click, flags)
    }
}

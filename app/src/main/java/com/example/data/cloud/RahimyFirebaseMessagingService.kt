package com.example.data.cloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Push Notification Service architecture for Rahimy Smart Commerce.
 * Handles push notifications for low-stock alerts, order status updates, and critical store events.
 */
class RahimyFirebaseMessagingService {

    companion object {
        private const val CHANNEL_ID = "rahimy_commerce_alerts"
        private const val CHANNEL_NAME = "Rahimy Commerce Alerts"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for sales, inventory alerts, and manager notices"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        fun showLocalNotification(context: Context, title: String, message: String, notificationId: Int = 1001) {
            createNotificationChannel(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notificationId, builder.build())
        }

        fun handleIncomingMessage(title: String?, body: String?) {
            Log.d("FCM", "Received message: $title - $body")
        }
    }
}

package com.frozenlab.hack

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MainMessagingService: FirebaseMessagingService() {

    private val broadcaster: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }

    override fun onCreate() {

        super.onCreate()

        if(Preferences.fcmToken.isBlank()) {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                Preferences.fcmToken = instanceIdResult.token
            }
        }
    }

    override fun onNewToken(token: String) {

        Preferences.fcmToken = token

        val intent = Intent(MainActivity.INTENT_NEW_FCM_TOKEN)
        intent.putExtra("fcm_token", token)
        broadcaster.sendBroadcast(intent)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.data["notification_id"]?.run {

            val intent = Intent(MainActivity.INTENT_NOTIFICATION_DATA)
            intent.putExtra("notification_id", this)
            broadcaster.sendBroadcast(intent)
        }

        message.notification?.let { notification ->

            sendNotification(
                notification.title ?: "",
                notification.body ?: "",
                message.data
            )
        }
    }

    private fun sendNotification(messageTitle: String, messageBody: String, data: Map<String, String>) {

        val intent = Intent(this, MainActivity::class.java)
        data.forEach { intent.putExtra(it.key, it.value) }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId       = getString(R.string.default_notification_channel_id)
        val channelName     = getString(R.string.default_notification_channel_name)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notifications)
            .setColor(ContextCompat.getColor(this, R.color.green))
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}
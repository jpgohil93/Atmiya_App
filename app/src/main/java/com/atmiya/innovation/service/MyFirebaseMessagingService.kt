package com.atmiya.innovation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.atmiya.innovation.MainActivity
import com.atmiya.innovation.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here.
        val title = remoteMessage.notification?.title ?: "New Notification"
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data
        
        val type = data["type"] // "funding_call", "wall_post", "mentor_video"
        val id = when(type) {
            "funding_call" -> data["fundingCallId"]
            "wall_post" -> data["postId"]
            "mentor_video" -> data["videoId"]
            else -> null
        }

        sendNotification(title, body, type, id)
    }

    override fun onNewToken(token: String) {
        // Send token to your app server.
    }

    private fun sendNotification(title: String, messageBody: String, type: String?, id: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        if (type != null && id != null) {
            intent.putExtra("navigation_destination", type)
            intent.putExtra("navigation_id", id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = when(type) {
            "funding_call" -> "channel_funding_calls"
            "wall_post" -> "channel_wall_posts"
            "mentor_video" -> "channel_mentor_videos"
            else -> "fcm_default_channel"
        }
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when(type) {
                "funding_call" -> "Funding Calls"
                "wall_post" -> "Wall Posts"
                "mentor_video" -> "Mentor Videos"
                else -> "General Notifications"
            }
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}

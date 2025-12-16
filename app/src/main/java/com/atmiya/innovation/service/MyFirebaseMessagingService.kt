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
        
        // Prevent Self-Notification
        val authorId = data["authorId"] ?: data["senderId"]
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (authorId != null && currentUserId != null && authorId == currentUserId) {
            return // Stop here, do not show notification
        }

        val type = data["type"] // "funding_call", "wall_post", "mentor_video", "connection_request", "connection_accepted"
        val senderRole = data["senderRole"] // "startup", "investor", "mentor"
        
        val (destType, destId) = when(type) {
            "funding_call" -> "funding_call" to data["fundingCallId"]
            "funding_call_update" -> "funding_call" to data["callId"]
            "wall_post" -> "wall_post" to data["postId"]
            "mentor_video" -> "mentor_video" to data["videoId"]
            "connection_request" -> "connection_requests" to null
            "connection_accepted" -> {
                val roleDetail = when(senderRole) {
                    "investor" -> "investor_detail"
                    "mentor" -> "mentor_detail"
                    "startup" -> "startup_detail"
                    else -> "profile_screen"
                }
                roleDetail to authorId
            }
            else -> null to null
        }
        
        sendNotification(title, body, destType, destId, type)
    }

    override fun onNewToken(token: String) {
        // Send token to your app server.
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).update("fcmToken", token)
        }
    }    // Implement token upload if user is logged in
    

    private fun sendNotification(title: String, messageBody: String, destType: String?, destId: String?, originalType: String?) {
        val intent = Intent(this, MainActivity::class.java)
        // Ensure we bring existing activity to front or create new one, triggering onNewIntent if needed
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        
        if (destType != null) {
            intent.putExtra("navigation_destination", destType)
            if (destId != null) intent.putExtra("navigation_id", destId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = when(originalType) {
            "funding_call" -> "channel_funding_calls"
            "wall_post" -> "channel_wall_posts"
            "mentor_video" -> "channel_mentor_videos"
            "connection_request", "connection_accepted" -> "channel_connections"
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
            val channelName = when(originalType) {
                "funding_call" -> "Funding Calls"
                "wall_post" -> "Wall Posts"
                "mentor_video" -> "Mentor Videos"
                "connection_request", "connection_accepted" -> "Connections"
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

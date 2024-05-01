package com.example.skyllsync

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationService (
    private val context: Context
    ) {
        private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        fun showDrinkNotification() {
            val activityIntent = Intent(context, MainActivity::class.java)
            val activityPendingIntent = PendingIntent.getActivity(
                context,
                1,
                activityIntent,
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.water_drop)
                .setContentTitle("Drink some water")
                .setContentText("Stay hydrated!")
                .setContentIntent(activityPendingIntent)
                .setPriority(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(activityPendingIntent, true)
                .build()
            notificationManager.notify(1, notification)
        }

        fun showStretchNotification() {
            val activityIntent = Intent(context, MainActivity::class.java)
            val activityPendingIntent = PendingIntent.getActivity(
                context,
                2,
                activityIntent,
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.sports_gymnastics)
                .setContentTitle("Great training")
                .setContentText("Now, time to stretch!")
                .setContentIntent(activityPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(2, notification)
        }

        companion object{
            const val CHANNEL_ID = "channel1"
        }
}
package com.example.skyllsync

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationService.CHANNEL_ID,
                "Channel 1",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Drink and stretch notification"
            channel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            val notificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }
}
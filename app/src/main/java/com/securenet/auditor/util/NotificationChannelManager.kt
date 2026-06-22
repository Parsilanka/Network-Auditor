package com.securenet.auditor.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelManager {
    const val MONITOR_CHANNEL_ID = "network_monitor"
    const val ALERT_CHANNEL_ID = "device_alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Network Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the background network monitor"
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Device Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when new devices are detected on the network"
                enableLights(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }
}

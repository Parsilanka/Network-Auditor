package com.securenet.auditor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.securenet.auditor.MainActivity
import com.securenet.auditor.R
import com.securenet.auditor.SecureNetApp
import com.securenet.auditor.data.prefs.EncryptedPrefsManager
import com.securenet.auditor.domain.model.ScanProgress
import com.securenet.auditor.network.SubnetScanner
import kotlinx.coroutines.flow.collect

class NetworkMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as SecureNetApp).container
            val prefs = container.encryptedPrefs
            
            val monitoringEnabled = prefs.getBoolSetting("monitoring_enabled") ?: false
            if (!monitoringEnabled) return Result.success()

            val scanner = container.subnetScanner
            val subnet = scanner.detectSubnet() 
                ?: return Result.success()

            val currentHosts = mutableListOf<String>()
            scanner.scanSubnet(subnet).collect { progress ->
                when (progress) {
                    is ScanProgress.HostFound -> {
                        currentHosts.add(progress.host.ipAddress)
                    }
                    is ScanProgress.Complete -> {
                        compareAndNotify(currentHosts, prefs)
                    }
                    else -> {}
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun compareAndNotify(
        currentHosts: List<String>,
        prefs: EncryptedPrefsManager
    ) {
        val previousHostsStr = prefs.getStringSetting("known_hosts") ?: ""
        val previousHosts = if (previousHostsStr.isEmpty())
            emptySet()
        else previousHostsStr.split(",").toSet()

        val currentHostSet = currentHosts.toSet()

        // New devices joined
        val newDevices = currentHostSet - previousHosts
        // Devices left
        val leftDevices = previousHosts - currentHostSet

        if (newDevices.isNotEmpty()) {
            sendNotification(
                title = "⚠ New Device Detected!",
                message = "${newDevices.size} new device(s) joined your network: " +
                    newDevices.take(3).joinToString(", "),
                notificationId = 1001
            )
        }

        if (leftDevices.isNotEmpty() && previousHosts.isNotEmpty()) {
            sendNotification(
                title = "Device Left Network",
                message = "${leftDevices.size} device(s) disconnected: " +
                    leftDevices.take(3).joinToString(", "),
                notificationId = 1002
            )
        }

        // Save current hosts as known baseline
        prefs.saveStringSetting("known_hosts", currentHostSet.joinToString(","))
        prefs.saveStringSetting("last_monitor_time", System.currentTimeMillis().toString())
    }

    private fun sendNotification(
        title: String,
        message: String,
        notificationId: Int
    ) {
        val channelId = "network_monitor_channel"

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Network Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for network changes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

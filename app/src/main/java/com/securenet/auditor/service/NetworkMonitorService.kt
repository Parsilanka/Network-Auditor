package com.securenet.auditor.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.securenet.auditor.R
import com.securenet.auditor.network.SubnetScanner
import com.securenet.auditor.util.NotificationChannelManager
import kotlinx.coroutines.*
import java.net.InetAddress

class NetworkMonitorService : Service() {

    private val knownDevices = mutableSetOf<String>()
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(
            "Network Monitor Active", 
            "Watching for new devices..."))
        loadKnownDevices()
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                val subnet = SubnetScanner(applicationContext).detectSubnet()
                if (subnet != null) {
                    scanForNewDevices(subnet)
                }
                // Use a dynamic interval if needed, but default to 5 minutes as requested
                delay(getScanInterval()) 
            }
        }
    }

    private fun getScanInterval(): Long {
        val prefs = applicationContext.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("scan_interval", 5 * 60 * 1000L)
    }

    private suspend fun scanForNewDevices(subnet: String) {
        val currentDevices = mutableSetOf<String>()
        coroutineScope {
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    val ip = "$subnet.$i"
                    try {
                        if (InetAddress.getByName(ip).isReachable(500)) {
                            currentDevices.add(ip)
                        }
                    } catch (e: Exception) {}
                }
            }.awaitAll()
        }

        val whitelist = getWhitelist()
        val newDevices = currentDevices - knownDevices - whitelist
        
        newDevices.forEach { ip ->
            sendNewDeviceNotification(ip)
            saveAlert(ip)
        }

        knownDevices.clear()
        knownDevices.addAll(currentDevices)
        saveKnownDevices()
    }

    private fun sendNewDeviceNotification(ip: String) {
        val notification = NotificationCompat.Builder(this, NotificationChannelManager.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠ New Device Detected")
            .setContentText("Unknown device joined: $ip")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(this).notify(ip.hashCode(), notification)
        } catch (e: SecurityException) {
            // Handle missing permission if on Android 13+ and not granted yet
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, NotificationChannelManager.MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun loadKnownDevices() {
        val prefs = applicationContext.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("known_devices", emptySet())
        knownDevices.addAll(saved ?: emptySet())
    }

    private fun saveKnownDevices() {
        applicationContext.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("known_devices", knownDevices)
            .apply()
    }

    private fun getWhitelist(): Set<String> {
        val prefs = applicationContext.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
    }

    private fun saveAlert(ip: String) {
        val prefs = applicationContext.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
        val alerts = prefs.getStringSet("alerts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        // Save as "ip|timestamp|wasAcknowledged"
        alerts.add("$ip|${System.currentTimeMillis()}|false")
        prefs.edit().putStringSet("alerts", alerts).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
            context.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("monitor_enabled", true).apply()
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NetworkMonitorService::class.java))
            context.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("monitor_enabled", false).apply()
        }
    }
}

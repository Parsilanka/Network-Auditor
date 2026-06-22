package com.securenet.auditor.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.securenet.auditor.data.db.ArpDao
import com.securenet.auditor.data.db.ArpEntity
import com.securenet.auditor.AppContainer
import com.securenet.auditor.R
import com.securenet.auditor.network.ArpScanner
import com.securenet.auditor.network.Pinger
import com.securenet.auditor.network.SubnetScanner
import com.securenet.auditor.util.NotificationChannelManager
import kotlinx.coroutines.*
import java.net.InetAddress

class NetworkMonitorService : Service() {

    private val knownDevices = mutableSetOf<String>()
    private var monitorJob: Job? = null
    private var packetLossJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private lateinit var arpDao: ArpDao
    private lateinit var subnetScanner: SubnetScanner
    private val arpScanner = ArpScanner()
    private val pinger = Pinger()

    override fun onCreate() {
        super.onCreate()
        val container = (application as com.securenet.auditor.SecureNetApp).container
        arpDao = container.arpDao
        subnetScanner = container.subnetScanner
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(
            "Network Monitor Active", 
            "Watching for new devices and attacks..."))
        loadKnownDevices()
        startMonitoring()
        startPacketLossMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                val subnet = subnetScanner.detectSubnet()
                if (subnet != null) {
                    scanForNewDevices(subnet)
                    checkArpSpoofing()
                }
                delay(getScanInterval()) 
            }
        }
    }

    private fun startPacketLossMonitoring() {
        packetLossJob?.cancel()
        packetLossJob = scope.launch {
            while (isActive) {
                val gateway = getGatewayIp()
                if (gateway != null) {
                    val rtt = pinger.ping(gateway)
                    logPacketLoss(gateway, rtt)
                }
                delay(30000) // 30 seconds
            }
        }
    }

    private fun getGatewayIp(): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        return android.text.format.Formatter.formatIpAddress(dhcpInfo.gateway)
    }

    private fun logPacketLoss(ip: String, rtt: Long) {
        val prefs = applicationContext.getSharedPreferences("packet_loss_logs", Context.MODE_PRIVATE)
        val logs = prefs.getStringSet("logs", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        logs.add("${System.currentTimeMillis()}|$rtt")
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val filtered = logs.filter { 
            it.split("|")[0].toLong() > cutoff 
        }.toSet()
        prefs.edit().putStringSet("logs", filtered).apply()
    }

    private suspend fun checkArpSpoofing() {
        val currentArp = arpScanner.getArpTable()
        currentArp.forEach { (ip, mac) ->
            val stored = arpDao.getByIp(ip)
            if (stored != null && stored.macAddress != mac) {
                sendAlertNotification("⚠ ARP Spoofing Detected", "Device $ip MAC changed from ${stored.macAddress} to $mac")
                saveAlert("ARP_SPOOF|$ip|$mac")
            }
            arpDao.insert(ArpEntity(ip, mac, System.currentTimeMillis()))
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
        sendAlertNotification("⚠ New Device Detected", "Unknown device joined: $ip")
    }

    private fun sendAlertNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, NotificationChannelManager.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(this).notify(text.hashCode(), notification)
        } catch (e: SecurityException) {
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
        packetLossJob?.cancel()
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

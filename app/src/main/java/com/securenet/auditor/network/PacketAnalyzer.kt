package com.securenet.auditor.network

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.net.Inet6Address
import java.net.NetworkInterface

class PacketAnalyzer(private val context: Context) {

    data class NetworkSession(
        val packageName: String,
        val appName: String,
        val rxBytes: Long,
        val txBytes: Long,
        val rxPackets: Long,
        val txPackets: Long,
        val timestamp: Long
    )

    data class ConnectionInfo(
        val localAddress: String,
        val remoteAddress: String,
        val state: String,
        val protocol: String
    )

    @RequiresApi(Build.VERSION_CODES.M)
    fun getNetworkStats(
        hours: Int = 24
    ): List<NetworkSession> {
        val networkStatsManager = context
            .getSystemService(
                Context.NETWORK_STATS_SERVICE)
            as NetworkStatsManager

        val packageManager = context.packageManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (hours * 3600000L)

        val sessions = mutableListOf<NetworkSession>()

        try {
            val stats = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null, startTime, endTime)

            while (stats.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                stats.getNextBucket(bucket)

                val uid = bucket.uid
                if (uid >= 10000) { // App UID range
                    val packages = packageManager
                        .getPackagesForUid(uid)
                    val packageName = packages
                        ?.firstOrNull() ?: "Unknown"
                    val appName = try {
                        val appInfo = packageManager
                            .getApplicationInfo(
                                packageName, 0)
                        packageManager
                            .getApplicationLabel(appInfo)
                            .toString()
                    } catch (e: Exception) { packageName }

                    sessions.add(NetworkSession(
                        packageName = packageName,
                        appName = appName,
                        rxBytes = bucket.rxBytes,
                        txBytes = bucket.txBytes,
                        rxPackets = bucket.rxPackets,
                        txPackets = bucket.txPackets,
                        timestamp = bucket.startTimeStamp
                    ))
                }
            }
            stats.close()
        } catch (e: Exception) {}

        return sessions
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                list.reduce { acc, session ->
                    acc.copy(
                        rxBytes = acc.rxBytes + 
                            session.rxBytes,
                        txBytes = acc.txBytes + 
                            session.txBytes,
                        rxPackets = acc.rxPackets + 
                            session.rxPackets,
                        txPackets = acc.txPackets + 
                            session.txPackets
                    )
                }
            }
            .sortedByDescending { it.rxBytes + it.txBytes }
    }

    fun getActiveConnectionInfo(
        context: Context
    ): List<ConnectionInfo> {
        val connections = mutableListOf<ConnectionInfo>()
        
        // Method 1: Try /proc/net/tcp6 and /proc/net/tcp
        // These may work on some devices
        listOf("/proc/net/tcp", "/proc/net/tcp6").forEach { path ->
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    file.readLines().drop(1)
                        .forEach { line ->
                        val parts = line.trim()
                            .split(Regex("\\s+"))
                        if (parts.size >= 4) {
                            val local = hexToAddress(parts[1])
                            val remote = hexToAddress(parts[2])
                            val state = tcpState(parts[3])
                            if (local != null && 
                                remote != null &&
                                !remote.startsWith("0.0.0.0") &&
                                !remote.startsWith(":::")) {
                                connections.add(ConnectionInfo(
                                    localAddress = local,
                                    remoteAddress = remote,
                                    state = state,
                                    protocol = if (path
                                        .contains("6")) 
                                        "TCP6" else "TCP"
                                ))
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        
        // Method 2: If proc files not readable,
        // use NetworkInterface to show active interfaces
        if (connections.isEmpty()) {
            try {
                val networkInterfaces = NetworkInterface
                    .getNetworkInterfaces()
                networkInterfaces?.asSequence()
                    ?.filter { it.isUp && !it.isLoopback }
                    ?.forEach { iface ->
                    iface.inetAddresses.asSequence()
                        .filter { !it.isLoopbackAddress }
                        .forEach { addr ->
                        connections.add(ConnectionInfo(
                            localAddress = 
                                "${addr.hostAddress}:*",
                            remoteAddress = "Active Interface",
                            state = "ACTIVE",
                            protocol = if (addr is 
                                Inet6Address) "IPv6" else "IPv4"
                        ))
                    }
                }
            } catch (e: Exception) {}
        }
        
        return connections
            .distinctBy { "${it.localAddress}${it.remoteAddress}" }
            .sortedBy { it.state }
    }

    private fun hexToAddress(hex: String): String? {
        return try {
            val parts = hex.split(":")
            val ipHex = parts[0]
            val portHex = parts[1]

            val ip = if (ipHex.length == 32) {
                // IPv6
                (0..7).joinToString(":") { i ->
                    ipHex.substring(i * 4, i * 4 + 4)
                }
            } else {
                // IPv4
                (0..3).map { i ->
                    Integer.parseInt(
                        ipHex.substring(i * 2, i * 2 + 2),
                        16)
                }.reversed().joinToString(".")
            }

            val port = Integer.parseInt(portHex, 16)
            "$ip:$port"
        } catch (e: Exception) { null }
    }

    private fun tcpState(stateHex: String): String {
        return when (stateHex) {
            "01" -> "ESTABLISHED"
            "02" -> "SYN_SENT"
            "03" -> "SYN_RECV"
            "04" -> "FIN_WAIT1"
            "05" -> "FIN_WAIT2"
            "06" -> "TIME_WAIT"
            "07" -> "CLOSE"
            "08" -> "CLOSE_WAIT"
            "09" -> "LAST_ACK"
            "0A" -> "LISTEN"
            "0B" -> "CLOSING"
            else -> "UNKNOWN"
        }
    }
}

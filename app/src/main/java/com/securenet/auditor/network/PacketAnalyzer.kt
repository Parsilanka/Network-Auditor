package com.securenet.auditor.network

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

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

    fun getActiveConnections(): List<ConnectionInfo> {
        val connections = mutableListOf<ConnectionInfo>()
        try {
            // Read from /proc/net/tcp
            val tcpFile = File("/proc/net/tcp")
            if (tcpFile.exists()) {
                tcpFile.readLines().drop(1).forEach { line ->
                    val parts = line.trim().split(
                        Regex("\\s+"))
                    if (parts.size >= 4) {
                        val localHex = parts[1]
                        val remoteHex = parts[2]
                        val stateHex = parts[3]

                        val local = hexToIpPort(localHex)
                        val remote = hexToIpPort(remoteHex)
                        val state = tcpState(stateHex)

                        if (local != null && 
                            remote != null) {
                            connections.add(ConnectionInfo(
                                localAddress = local,
                                remoteAddress = remote,
                                state = state,
                                protocol = "TCP"
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return connections
    }

    private fun hexToIpPort(hex: String): String? {
        return try {
            val parts = hex.split(":")
            val ipHex = parts[0]
            val portHex = parts[1]

            // Convert little-endian hex to IP
            val ip = (0..3).map { i ->
                Integer.parseInt(
                    ipHex.substring(i * 2, i * 2 + 2),
                    16)
            }.reversed().joinToString(".")

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

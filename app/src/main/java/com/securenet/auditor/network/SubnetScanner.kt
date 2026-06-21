package com.securenet.auditor.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.securenet.auditor.domain.model.HostInfo
import com.securenet.auditor.domain.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class SubnetScanner(private val context: Context) {

    fun detectSubnet(): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        if (dhcpInfo == null || dhcpInfo.gateway == 0) return null
        
        val gateway = dhcpInfo.gateway
        return "${gateway and 0xFF}.${(gateway shr 8) and 0xFF}.${(gateway shr 16) and 0xFF}"
    }

    fun scanSubnet(subnet: String): Flow<ScanProgress> = channelFlow {
        val results = Collections.synchronizedList(mutableListOf<HostInfo>())
        val currentCount = AtomicInteger(0)
        val semaphore = Semaphore(50)
        val startTime = System.currentTimeMillis()

        coroutineScope {
            (1..254).forEach { i ->
                launch {
                    semaphore.withPermit {
                        val ip = "$subnet.$i"
                        try {
                            val addr = InetAddress.getByName(ip)
                            val reachable = addr.isReachable(500)
                            if (reachable) {
                                val responseTime = System.currentTimeMillis() - startTime
                                val hostname = try {
                                    val canonical = addr.canonicalHostName
                                    if (canonical == ip) null else canonical
                                } catch (e: Exception) {
                                    null
                                }
                                val host = HostInfo(
                                    ipAddress = ip,
                                    hostname = hostname,
                                    macAddress = null,
                                    vendor = null,
                                    openPorts = emptyList(),
                                    isReachable = true,
                                    responseTimeMs = responseTime
                                )
                                results.add(host)
                                send(ScanProgress.HostFound(host))
                            }
                        } catch (e: Exception) {
                            // Ignore
                        } finally {
                            val current = currentCount.incrementAndGet()
                            send(ScanProgress.Scanning(current, 254, ip))
                        }
                    }
                }
            }
        }
        send(ScanProgress.Complete(results.toList(), System.currentTimeMillis() - startTime))
    }.flowOn(Dispatchers.IO)
}

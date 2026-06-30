package com.securenet.auditor.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class RogueApDetector(private val context: Context) {

    data class AccessPointInfo(
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,
        val frequency: Int,
        val capabilities: String,
        val channel: Int
    )

    data class RogueApAlert(
        val targetSsid: String,
        val legitimateBssid: String,
        val suspiciousBssid: String,
        val signalStrength: Int,
        val threatType: RogueThreatType,
        val description: String,
        val recommendation: String
    )

    enum class RogueThreatType {
        EVIL_TWIN,
        SSID_SPOOF,
        DEAUTH_ATTACK,
        OPEN_NETWORK_SPOOF,
        SUSPICIOUS_SIGNAL
    }

    data class RogueApScanResult(
        val currentSsid: String,
        val currentBssid: String,
        val allNetworks: List<AccessPointInfo>,
        val alerts: List<RogueApAlert>
    )

    data class ApScanResult(
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,
        val capabilities: String,
        val suspicionReasons: List<String> = emptyList()
    )

    data class RogueApReport(
        val riskLevel: String,
        val suspiciousNetworks: List<ApScanResult>,
        val nearbyNetworks: List<ApScanResult>,
        val connectedSsid: String?,
        val connectedBssid: String?
    )

    @SuppressLint("MissingPermission")
    suspend fun scanForRogueAps(
        context: Context
    ): RogueApScanResult = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) 
            as WifiManager

        // Get current connection info FIRST before scanning
        val currentInfo = wifiManager.connectionInfo
        val currentSsid = currentInfo?.ssid
            ?.removeSurrounding("\"") ?: "Unknown"
        val currentBssid = currentInfo?.bssid ?: "N/A"

        // Trigger scan and WAIT for results using callback
        val scanResults = suspendCancellableCoroutine<List<ScanResult>> { continuation ->
            val scanReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {}
                    
                    try {
                        val results = wifiManager.scanResults
                        if (continuation.isActive) {
                            continuation.resume(results)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
                }
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(scanReceiver, intentFilter)

            val scanStarted = wifiManager.startScan()
            
            if (!scanStarted) {
                // Scan failed to start, use cached results
                try {
                    context.unregisterReceiver(scanReceiver)
                } catch (e: Exception) {}
                
                if (continuation.isActive) {
                    continuation.resume(wifiManager.scanResults)
                }
            }

            // Timeout fallback or cancellation
            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(scanReceiver)
                } catch (e: Exception) {}
            }
        }

        // Now analyze the results we actually received
        val alerts = analyzeForRogueAps(scanResults, currentSsid, currentBssid)

        RogueApScanResult(
            currentSsid = currentSsid,
            currentBssid = currentBssid,
            allNetworks = scanResults.map { result ->
                AccessPointInfo(
                    ssid = result.SSID.removeSurrounding("\""),
                    bssid = result.BSSID,
                    signalStrength = result.level,
                    frequency = result.frequency,
                    capabilities = result.capabilities,
                    channel = frequencyToChannel(result.frequency)
                )
            }.filter { it.ssid.isNotEmpty() }
                .distinctBy { it.bssid }
                .sortedByDescending { it.signalStrength },
            alerts = alerts
        )
    }

    private fun analyzeForRogueAps(
        scanResults: List<ScanResult>,
        currentSsid: String,
        currentBssid: String
    ): List<RogueApAlert> {
        val alerts = mutableListOf<RogueApAlert>()
        val networksBySsid = scanResults.groupBy { it.SSID.removeSurrounding("\"") }

        networksBySsid.forEach { (ssid, networks) ->
            if (ssid == currentSsid && networks.size > 1) {
                networks.forEach { ap ->
                    val bssid = ap.BSSID
                    if (bssid != currentBssid) {
                        val currentSecurity = networks.find { it.BSSID == currentBssid }?.capabilities ?: ""
                        val hasWeakerSecurity = currentSecurity.contains("WPA") && !ap.capabilities.contains("WPA")

                        if (hasWeakerSecurity) {
                            alerts.add(RogueApAlert(
                                targetSsid = ssid,
                                legitimateBssid = currentBssid,
                                suspiciousBssid = bssid,
                                signalStrength = ap.level,
                                threatType = RogueThreatType.EVIL_TWIN,
                                description = "Suspicious AP detected: Same SSID as your network ($ssid) but weaker security. This is a classic evil twin attack pattern.",
                                recommendation = "Do not connect to this network. Disconnect immediately if connected."
                            ))
                        }

                        if (ap.level > -30) {
                            alerts.add(RogueApAlert(
                                targetSsid = ssid,
                                legitimateBssid = currentBssid,
                                suspiciousBssid = bssid,
                                signalStrength = ap.level,
                                threatType = RogueThreatType.SUSPICIOUS_SIGNAL,
                                description = "Unusually strong signal detected from duplicate SSID ($ssid). This may indicate an AP placed nearby to force connections.",
                                recommendation = "Verify this is a legitimate access point from your network provider."
                            ))
                        }
                    }
                }
            }

            if (ssid != currentSsid && isSimilarSsid(ssid, currentSsid)) {
                val openNetworks = networks.filter { !it.capabilities.contains("WPA") && !it.capabilities.contains("WEP") }
                openNetworks.forEach { ap ->
                    alerts.add(RogueApAlert(
                        targetSsid = ssid,
                        legitimateBssid = currentBssid,
                        suspiciousBssid = ap.BSSID,
                        signalStrength = ap.level,
                        threatType = RogueThreatType.SSID_SPOOF,
                        description = "Open network '$ssid' has similar name to your network '$currentSsid'. This may be an attempt to trick you into connecting to an unencrypted network.",
                        recommendation = "Do not connect to '$ssid'. Connect only to your known secured network."
                    ))
                }
            }
        }
        return alerts
    }

    private fun frequencyToChannel(freq: Int): Int {
        return when {
            freq in 2412..2484 -> (freq - 2412) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            else -> 0
        }
    }

    private fun isSimilarSsid(ssid1: String, ssid2: String): Boolean {
        if (ssid1.isEmpty() || ssid2.isEmpty()) return false
        val lower1 = ssid1.lowercase().trim()
        val lower2 = ssid2.lowercase().trim()
        if (lower1.contains(lower2) || lower2.contains(lower1)) return true
        return levenshteinDistance(lower1, lower2) <= 2
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                else minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + 1)
            }
        }
        return dp[s1.length][s2.length]
    }
}

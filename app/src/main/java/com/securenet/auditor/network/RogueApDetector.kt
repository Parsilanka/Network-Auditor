package com.securenet.auditor.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager

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
    fun scanForRogueAps(): List<RogueApAlert> {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE)
            as WifiManager
        val currentInfo = wifiManager.connectionInfo
        val currentSsid = currentInfo?.ssid
            ?.removeSurrounding("\"") ?: return emptyList()
        val currentBssid = currentInfo.bssid

        @Suppress("DEPRECATION")
        wifiManager.startScan()
        val scanResults = wifiManager.scanResults

        val alerts = mutableListOf<RogueApAlert>()

        // Group all networks by SSID
        val networksBySsid = scanResults
            .groupBy { it.SSID.removeSurrounding("\"") }

        networksBySsid.forEach { (ssid, networks) ->
            if (ssid == currentSsid && networks.size > 1) {
                // Multiple APs with same SSID — check for
                // evil twin indicators

                networks.forEach { ap ->
                    val bssid = ap.BSSID

                    // Check 1: Same SSID different security
                    val currentSecurity = networks.find {
                        it.BSSID == currentBssid
                    }?.capabilities ?: ""
                    
                    val hasWeakerSecurity = 
                        currentSecurity.contains("WPA") &&
                        !ap.capabilities.contains("WPA")

                    if (hasWeakerSecurity &&
                        bssid != currentBssid) {
                        alerts.add(RogueApAlert(
                            targetSsid = ssid,
                            legitimateBssid = currentBssid ?: "Unknown",
                            suspiciousBssid = bssid,
                            signalStrength = ap.level,
                            threatType = 
                                RogueThreatType.EVIL_TWIN,
                            description = "Suspicious AP " +
                                "detected: Same SSID as your " +
                                "network ($ssid) but weaker " +
                                "security (open/WEP vs your " +
                                "WPA2). This is a classic " +
                                "evil twin attack pattern.",
                            recommendation = "Do not connect " +
                                "to this network. Disconnect " +
                                "immediately if connected."
                        ))
                    }

                    // Check 2: Unusually strong signal
                    // (AP placed very close deliberately)
                    if (ap.level > -30 && 
                        bssid != currentBssid &&
                        ssid == currentSsid) {
                        alerts.add(RogueApAlert(
                            targetSsid = ssid,
                            legitimateBssid = currentBssid ?: "Unknown",
                            suspiciousBssid = bssid,
                            signalStrength = ap.level,
                            threatType = 
                                RogueThreatType
                                    .SUSPICIOUS_SIGNAL,
                            description = "Unusually strong " +
                                "signal detected from duplicate " +
                                "SSID ($ssid). Signal: " +
                                "${ap.level} dBm. This may " +
                                "indicate an AP placed nearby " +
                                "to force connections.",
                            recommendation = "Verify this is " +
                                "a legitimate access point " +
                                "from your network provider."
                        ))
                    }
                }
            }

            // Check 3: Open network with same/similar SSID
            // as your secured network
            if (ssid != currentSsid &&
                isSimilarSsid(ssid, currentSsid)) {
                val openNetworks = networks.filter {
                    !it.capabilities.contains("WPA") &&
                    !it.capabilities.contains("WEP")
                }
                openNetworks.forEach { ap ->
                    alerts.add(RogueApAlert(
                        targetSsid = ssid,
                        legitimateBssid = currentBssid ?: "Unknown",
                        suspiciousBssid = ap.BSSID,
                        signalStrength = ap.level,
                        threatType = 
                            RogueThreatType.SSID_SPOOF,
                        description = "Open network '$ssid' " +
                            "has similar name to your network " +
                            "'$currentSsid'. This may be an " +
                            "attempt to trick you into " +
                            "connecting to an unencrypted " +
                            "network.",
                        recommendation = "Do not connect to " +
                            "'$ssid'. Connect only to your " +
                            "known secured network."
                    ))
                }
            }
        }

        return alerts
    }

    private fun isSimilarSsid(
        ssid1: String, ssid2: String
    ): Boolean {
        if (ssid1.isEmpty() || ssid2.isEmpty()) 
            return false
        val lower1 = ssid1.lowercase().trim()
        val lower2 = ssid2.lowercase().trim()

        // Check if one contains the other
        if (lower1.contains(lower2) || 
            lower2.contains(lower1)) return true

        // Check Levenshtein distance <= 2
        return levenshteinDistance(lower1, lower2) <= 2
    }

    private fun levenshteinDistance(
        s1: String, s2: String
    ): Int {
        val dp = Array(s1.length + 1) {
            IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i-1] == s2[j-1])
                    dp[i-1][j-1]
                else minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + 1)
            }
        }
        return dp[s1.length][s2.length]
    }
}

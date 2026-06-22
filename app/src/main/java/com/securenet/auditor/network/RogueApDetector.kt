package com.securenet.auditor.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build

class RogueApDetector(private val context: Context) {

    data class ApScanResult(
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,
        val frequency: Int,
        val capabilities: String,
        val isSuspicious: Boolean,
        val suspicionReasons: List<String>
    )

    data class RogueApReport(
        val scannedAt: Long,
        val connectedSsid: String?,
        val connectedBssid: String?,
        val nearbyNetworks: List<ApScanResult>,
        val duplicateSsids: List<List<ApScanResult>>,
        val suspiciousNetworks: List<ApScanResult>,
        val riskLevel: String
    )

    @SuppressLint("MissingPermission")
    fun scanForRogueAps(): RogueApReport {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        val scanResults = wifiManager.scanResults ?: emptyList()
        val connectedInfo = wifiManager.connectionInfo

        val processedResults = scanResults.map { result ->
            val reasons = mutableListOf<String>()
            var suspicious = false

            // Check 1: Open network (no encryption)
            if (!result.capabilities.contains("WPA") && 
                !result.capabilities.contains("WEP")) {
                reasons.add("Open network — no encryption")
                suspicious = true
            }

            // Check 2: Hidden SSID with strong signal (potential evil twin)
            if (result.SSID.isNullOrEmpty() && result.level > -60) {
                reasons.add("Hidden SSID with strong signal")
                suspicious = true
            }

            // Check 3: Same SSID as connected network but different BSSID (potential evil twin)
            val currentSsid = connectedInfo?.ssid?.removeSurrounding("\"")
            if (result.SSID == currentSsid && result.BSSID != connectedInfo?.bssid) {
                reasons.add("Same name as your network but different access point — possible evil twin!")
                suspicious = true
            }

            // Check 4: Unusually strong signal (device placed very close to you)
            if (result.level > -30) {
                reasons.add("Unusually strong signal (${result.level} dBm) — device may be very close")
                suspicious = true
            }

            ApScanResult(
                ssid = result.SSID ?: "<Hidden>",
                bssid = result.BSSID ?: "Unknown",
                signalStrength = result.level,
                frequency = result.frequency,
                capabilities = result.capabilities,
                isSuspicious = suspicious,
                suspicionReasons = reasons
            )
        }

        // Find duplicate SSIDs (same name, different BSSID)
        val ssidGroups = processedResults
            .filter { it.ssid.isNotBlank() && it.ssid != "<Hidden>" }
            .groupBy { it.ssid }
            .filter { it.value.size > 1 }
            .values.toList()

        val riskLevel = when {
            processedResults.any { r -> 
                r.suspicionReasons.any { it.contains("evil twin") }
            } -> "CRITICAL"
            ssidGroups.isNotEmpty() -> "HIGH"
            processedResults.any { it.isSuspicious } -> "MEDIUM"
            else -> "LOW"
        }

        return RogueApReport(
            scannedAt = System.currentTimeMillis(),
            connectedSsid = connectedInfo?.ssid?.removeSurrounding("\""),
            connectedBssid = connectedInfo?.bssid,
            nearbyNetworks = processedResults,
            duplicateSsids = ssidGroups,
            suspiciousNetworks = processedResults.filter { it.isSuspicious },
            riskLevel = riskLevel
        )
    }
}
